package dev.keel.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.keel.model.DecompositionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

@Component
@ConditionalOnProperty(name = "keel.engine", havingValue = "llm")
public class LlmApiDecompositionEngine implements DecompositionEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(LlmApiDecompositionEngine.class);

    private static final String OPENAI_DEFAULT_BASE_URL = "https://api.openai.com";
    private static final String OPENAI_RESPONSES_PATH = "/v1/responses";
    private static final String ANTHROPIC_DEFAULT_BASE_URL = "https://api.anthropic.com";
    private static final String ANTHROPIC_MESSAGES_PATH = "/v1/messages";
    private static final int FILE_STRUCTURE_LIMIT = 300;
    private static final int ROUTING_FILE_LIMIT = 5;
    private static final int ROUTING_FILE_LINE_LIMIT = 200;
    private static final int REQUIREMENT_FILE_LIMIT = 10;
    private static final int REQUIREMENT_FILE_LINE_LIMIT = 150;

    private final LlmProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public LlmApiDecompositionEngine(LlmProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public DecompositionResult decompose(String requirement) {
        String repoContext = buildRepoContext(properties.getRepoPath(), requirement);
        String prompt = buildPrompt(requirement, repoContext);
        String responseText = switch (provider()) {
            case "openai" -> callOpenAi(prompt);
            case "anthropic" -> callAnthropic(prompt);
            default -> throw new IllegalStateException("keel.llm.provider must be openai or anthropic");
        };

        // вырезать всё до первой { — Claude иногда добавляет текст перед JSON
        String json = extractJson(responseText);

        try {
            return objectMapper.readValue(json, DecompositionResult.class);
        } catch (IOException e) {
            throw new IllegalStateException("LLM returned invalid DecompositionResult JSON: " + responseText, e);
        }
    }

    private String extractJson(String text) {
        if (text == null)
            return "";
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start == -1 || end == -1 || end < start) {
            throw new IllegalStateException("LLM response does not contain JSON object: " + text);
        }
        return text.substring(start, end + 1);
    }

    private String buildRepoContext(String repoPath) {
        return buildRepoContext(repoPath, "");
    }

    private String buildRepoContext(String repoPath, String requirement) {
        if (repoPath == null || repoPath.isBlank()) {
            return "";
        }

        Path repo = Path.of(repoPath).toAbsolutePath().normalize();
        if (!Files.isDirectory(repo)) {
            LOGGER.warn("Repository context path does not point to an existing directory: {}", repo);
            return "";
        }

        StringBuilder context = new StringBuilder();
        appendFileStructure(context, repo);
        appendRoutingFiles(context, repo);
        appendRequirementFiles(context, repo, requirement);
        return context.toString().trim();
    }

    private void appendFileStructure(StringBuilder context, Path repo) {
        context.append("=== СТРУКТУРА ФАЙЛОВ РЕПОЗИТОРИЯ ===\n");

        List<String> command = List.of(
                "find", repo.toString(),
                "-type", "f",
                "(", "-name", "*.php",
                "-o", "-name", "*.java",
                "-o", "-name", "*.ts",
                "-o", "-name", "*.py",
                "-o", "-name", "*.go", ")",
                "-not", "-path", "*/vendor/*",
                "-not", "-path", "*/node_modules/*",
                "-not", "-path", "*/.git/*");

        List<String> files = runCommandLines(command, FILE_STRUCTURE_LIMIT, List.of(0));
        context.append(String.join("\n", files)).append("\n\n");
    }

    private void appendRoutingFiles(StringBuilder context, Path repo) {
        context.append("=== КЛЮЧЕВЫЕ ROUTING/ENTRY POINT ФАЙЛЫ ===\n");

        for (Path file : findRoutingFiles(repo)) {
            appendFileExcerpt(context, repo, file, ROUTING_FILE_LINE_LIMIT);
        }

        context.append("\n");
    }

    private void appendRequirementFiles(StringBuilder context, Path repo, String requirement) {
        context.append("=== ФАЙЛЫ СВЯЗАННЫЕ С ТРЕБОВАНИЕМ ===\n");

        String keywords = firstWords(requirement, 3);
        if (keywords.isBlank()) {
            context.append("\n");
            return;
        }

        List<String> command = List.of(
                "grep", "-r", "-l",
                "--include=*.php",
                "--include=*.java",
                "--include=*.ts",
                keywords,
                repo.toString());

        List<String> files = runCommandLines(command, REQUIREMENT_FILE_LIMIT, List.of(0, 1));
        for (String file : files) {
            if (file == null || file.isBlank()) {
                continue;
            }

            Path path = Path.of(file).toAbsolutePath().normalize();
            if (Files.isRegularFile(path)) {
                appendFileExcerpt(context, repo, path, REQUIREMENT_FILE_LINE_LIMIT);
            }
        }

        context.append("\n");
    }

    private List<Path> findRoutingFiles(Path repo) {
        try (Stream<Path> paths = Files.walk(repo)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> !isIgnoredPath(path))
                    .filter(this::isRoutingFile)
                    .limit(ROUTING_FILE_LIMIT)
                    .toList();
        } catch (IOException e) {
            LOGGER.warn("Failed to find routing/entry point files in {}", repo, e);
            return List.of();
        }
    }

    private boolean isRoutingFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.contains("router")
                || fileName.contains("routing")
                || fileName.contains("routes")
                || fileName.contains("controller");
    }

    private boolean isIgnoredPath(Path path) {
        for (Path part : path) {
            String name = part.toString();
            if ("vendor".equals(name) || "node_modules".equals(name) || ".git".equals(name)) {
                return true;
            }
        }

        return false;
    }

    private void appendFileExcerpt(StringBuilder context, Path repo, Path file, int lineLimit) {
        context.append("--- ").append(relativePath(repo, file)).append(" ---\n");
        String content = readFileLines(file, lineLimit);
        if (!content.isBlank()) {
            context.append(content).append("\n");
        }
        context.append("\n");
    }

    private String readFileLines(Path file, int lineLimit) {
        StringBuilder content = new StringBuilder();

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lines = 0;
            while (lines < lineLimit && (line = reader.readLine()) != null) {
                content.append(line).append("\n");
                lines++;
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to read repository context file {}", file, e);
            return "";
        }

        return content.toString().stripTrailing();
    }

    private List<String> runCommandLines(List<String> command, int maxLines, List<Integer> successfulExitCodes) {
        try {
            Process process = new ProcessBuilder(command).start();
            CompletableFuture<List<String>> stdout = readFirstLinesAsync(process.getInputStream(), maxLines);
            CompletableFuture<String> stderr = readTextAsync(process.getErrorStream());

            int exitCode = process.waitFor();
            List<String> output = stdout.get();
            String error = stderr.get().trim();

            if (!successfulExitCodes.contains(exitCode)) {
                LOGGER.warn(
                        "Repository context command exited with code {}: {}{}",
                        exitCode,
                        command,
                        error.isBlank() ? "" : "\nstderr: " + error);
            }

            return output;
        } catch (IOException e) {
            LOGGER.warn("Failed to start repository context command: {}", command, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Repository context command was interrupted: {}", command, e);
        } catch (ExecutionException e) {
            LOGGER.warn("Failed to read repository context command output: {}", command, e.getCause());
        }

        return List.of();
    }

    private CompletableFuture<List<String>> readFirstLinesAsync(InputStream inputStream, int maxLines) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> lines = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (lines.size() < maxLines) {
                        lines.add(line);
                    }
                }
            } catch (IOException e) {
                throw new CompletionException(e);
            }

            return lines;
        });
    }

    private CompletableFuture<String> readTextAsync(InputStream inputStream) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }

    private String firstWords(String text, int maxWords) {
        if (text == null || text.isBlank()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        String[] words = text.trim().split("\\s+");

        int wordCount = 0;
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }

            if (result.length() > 0) {
                result.append(" ");
            }

            result.append(word);
            wordCount++;

            if (wordCount == maxWords) {
                break;
            }
        }

        return result.toString();
    }

    private String relativePath(Path repo, Path file) {
        try {
            return repo.relativize(file.toAbsolutePath().normalize()).toString();
        } catch (IllegalArgumentException e) {
            return file.toString();
        }
    }

    private String callOpenAi(String prompt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", requireNonBlank(properties.getModel(), "keel.llm.model"));
        body.put("input", prompt);

        String responseBody = restClient.post()
                .uri(endpoint(OPENAI_DEFAULT_BASE_URL, OPENAI_RESPONSES_PATH))
                .headers(headers -> applyOpenAiHeaders(headers,
                        requireNonBlank(properties.getApiKey(), "keel.llm.api-key")))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(
                        status -> !status.is2xxSuccessful(),
                        (request, response) -> {
                            String errorBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                            throw new IllegalStateException(errorBody);
                        })
                .body(String.class);

        return readOpenAiText(responseBody);
    }

    private String callAnthropic(String prompt) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", requireNonBlank(properties.getModel(), "keel.llm.model"));
        body.put("max_tokens", 8096);
        body.put("messages", List.of(message));

        String responseBody = restClient.post()
                .uri(endpoint(ANTHROPIC_DEFAULT_BASE_URL, ANTHROPIC_MESSAGES_PATH))
                .headers(headers -> applyAnthropicHeaders(headers,
                        requireNonBlank(properties.getApiKey(), "keel.llm.api-key")))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(
                        status -> !status.is2xxSuccessful(),
                        (request, response) -> {
                            String errorBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                            throw new IllegalStateException(errorBody);
                        })
                .body(String.class);

        return readAnthropicText(responseBody);
    }

    private void applyOpenAiHeaders(HttpHeaders headers, String apiKey) {
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    private void applyAnthropicHeaders(HttpHeaders headers, String apiKey) {
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    private String readOpenAiText(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("OpenAI response body is empty");
        }

        try {
            JsonNode text = objectMapper.readTree(responseBody)
                    .path("output")
                    .path(0)
                    .path("content")
                    .path(0)
                    .path("text");

            if (text.isMissingNode() || text.isNull()) {
                throw new IllegalStateException(
                        "OpenAI response does not contain output[0].content[0].text: " + responseBody);
            }

            return text.asText();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("OpenAI response JSON is invalid: " + responseBody, e);
        }
    }

    private String readAnthropicText(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("Anthropic response body is empty");
        }

        try {
            JsonNode text = objectMapper.readTree(responseBody)
                    .path("content")
                    .path(0)
                    .path("text");

            if (text.isMissingNode() || text.isNull()) {
                throw new IllegalStateException("Anthropic response does not contain content[0].text: " + responseBody);
            }

            return text.asText();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Anthropic response JSON is invalid: " + responseBody, e);
        }
    }

    private String endpoint(String defaultBaseUrl, String path) {
        if (properties.getBaseUrl() == null || properties.getBaseUrl().isBlank()) {
            return defaultBaseUrl + path;
        }

        String baseUrl = trimTrailingSlash(properties.getBaseUrl().trim());

        if (baseUrl.endsWith(path)) {
            return baseUrl;
        }

        if (baseUrl.endsWith("/v1")) {
            return baseUrl + path.substring("/v1".length());
        }

        return baseUrl + path;
    }

    private String trimTrailingSlash(String value) {
        String result = value;

        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }

    private String provider() {
        if (properties.getProvider() == null || properties.getProvider().isBlank()) {
            return "openai";
        }

        return properties.getProvider().trim().toLowerCase(Locale.ROOT);
    }

    private String requireNonBlank(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " must not be blank");
        }

        return value.trim();
    }

    private String repoContextBlock(String repoContext) {
        if (repoContext == null || repoContext.isBlank()) {
            return "";
        }

        return "=== КОДОВАЯ БАЗА ===\n" + repoContext.trim() + "\n\n";
    }

    private String buildPrompt(String requirement, String repoContext) {
        return """
                You are a requirement decomposition engine for a software delivery planning tool.

                Сначала исследуй кодовую базу в своей рабочей директории: прочитай ключевые исходные файлы,
                пойми существующую архитектуру и что уже реализовано. Не отвечай только по тексту требования.

                Сначала агент ОБЯЗАН читать реальный код, как описано выше, но результат разложи
                по трем слоям:
                1. title этапа, title пункта и description пункта — только человеческий смысл и намерение.
                2. considerations — короткие человеческие предупреждения, риски и подсказки переиспользования.
                3. devNotes — технические детали из репозитория.

                Для КАЖДОЙ задачи обязательно верни оба поля:
                - title — короткий заголовок одной строкой, человеческим языком, без кода.
                - description — 2-4 предложения, ЧЕЛОВЕЧЕСКИМ языком: что именно нужно сделать
                  и каким должен быть результат. Это мини-ТЗ, понятное и аналитику, и агенту.
                  Это НЕ список рисков, потому что риски должны быть в considerations. Это НЕ код.

                В title этапа, title пункта и description пункта НЕ ДОЛЖНО быть ни одного имени файла, класса,
                метода, эндпоинта, таблицы, DTO, пакета или другого технического идентификатора.
                Они должны быть понятны не-техническому читателю. Имена файлов, классов, эндпоинтов
                и других технических сущностей указывай только в devNotes. Пример допустимого title:
                "Реализовать подачу и согласование заявки".

                considerations заполняй человеческим языком из анализа кода, но БЕЗ имен файлов,
                классов, методов, эндпоинтов, таблиц, DTO, пакетов и других технических идентификаторов.
                Примеры тона: "в системе уже есть похожий механизм — проверить, можно ли переиспользовать";
                "легко спутать с соседним модулем — конфликт доменов"; "возможно, часть уже есть".

                devNotes — единственное место, где можно и нужно указывать реальные файлы, классы,
                методы, эндпоинты, DTO, таблицы и другие технические детали из кодовой базы.
                Не помещай технические имена ни в какие другие поля.

                integrationRisks оставь верхнеуровневым списком сквозных человеческих рисков интеграции
                из настоящего кода: авторизация, хранение файлов, внешние интеграции, соседние домены.
                Не превращай integrationRisks в список файлов или классов.

                Decompose the requirement by business/domain areas.
                Order stages by user/business value: first the core functionality that delivers the main
                user outcome, then secondary work such as roles/administration and reporting.
                Preserve real dependencies when they affect delivery order.
                Split by the essence of the work, not by technical layers.
                Do not create stages like controller, service, repository, database, tests, or UI unless they are true domain slices.

                СОРАЗМЕРНОСТЬ. Количество этапов и задач должно соответствовать реальному масштабу
                требования, а не дробиться искусственно и не склеиваться чрезмерно.
                - Маленькое требование (одно поле, один экран, одна локальная функция) — как правило
                  1 этап и 2-4 задачи. НЕ создавай отдельный этап ради одной-двух мелких задач;
                  смежные эффекты выноси в considerations существующих задач.
                - Если в требовании есть несколько ЯВНО ПЕРЕЧИСЛЕННЫХ самостоятельных сценариев,
                  случаев, триггеров или событий, каждый такой самостоятельный сценарий должен
                  оставаться ОТДЕЛЬНОЙ задачей, а не склеиваться с другими в одну.
                  Не объединяй разные перечисленные сценарии под общим заголовком ради компактности.
                - Группируй задачи в этапы по смыслу, но число задач должно покрывать все явно
                  перечисленные в требовании сценарии по отдельности. Соразмерность ограничивает
                  ИСКУССТВЕННОЕ дробление, а не склейку реально разных сценариев.
                - Крупное требование (много сценариев, несколько подсистем) — несколько этапов, как и раньше.
                - Не выделяй документацию и тесты в отдельный этап для мелких требований — это пункт, а не этап.

                КУДА УБИРАТЬ СМЕЖНЫЕ ЭФФЕКТЫ. Продолжай глубоко анализировать код и находить смежные места,
                которые затронет требование: снимки данных, автозаполнение, соседние интеграции, права,
                совместимость. Но на мелких требованиях НЕ превращай каждый такой эффект в отдельную задачу
                или этап — выноси его в considerations существующей задачи как предупреждение "не забыть проверить".
                Глубина анализа сохраняется, но изложение остается компактным.

                Every work item must have title, description, and size: S, M, or L.
                Every work item must include considerations and devNotes arrays. They may be empty arrays.
                Include a focused list of integration risks.

                You MUST return ONLY raw JSON.
                Do not return markdown.
                Do not wrap JSON in code fences.
                Do not add any text before or after the JSON.
                The JSON must strictly match the DecompositionResult structure and exact field names.
                Do not add extra fields.

                DecompositionResult JSON schema example:
                {
                  "stages": [
                    {
                      "title": "Human-readable stage title without technical identifiers",
                      "items": [
                        {
                          "title": "Short human-readable work item title without code or technical identifiers",
                          "description": "Describe what the user should be able to do and how the completed result should behave. Explain the expected outcome in plain language so both analysts and implementation agents understand the task.",
                          "size": "S",
                          "considerations": [
                            "Human-readable warning or reuse hint without technical identifiers"
                          ],
                          "devNotes": [
                            "Concrete real file/class/endpoint/method from the repository"
                          ]
                        }
                      ]
                    }
                  ],
                  "integrationRisks": [
                    "Concrete integration risk"
                  ]
                }

                %sRequirement:
                %s
                """
                .formatted(repoContextBlock(repoContext), requirement);
    }
}
