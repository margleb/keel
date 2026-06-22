package dev.keel.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.keel.model.DecompositionResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@Component
@ConditionalOnProperty(name = "keel.engine", havingValue = "codex")
public class CodexDecompositionEngine implements DecompositionEngine {

    private final ObjectMapper objectMapper;
    private final CodexProperties properties;

    public CodexDecompositionEngine(ObjectMapper objectMapper, CodexProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public DecompositionResult decompose(String requirement) {
        String prompt = buildPrompt(requirement);
        String stdout = runCodex(prompt);

        try {
            return objectMapper.readValue(stdout, DecompositionResult.class);
        } catch (IOException e) {
            throw new IllegalStateException("Codex returned invalid DecompositionResult JSON: " + stdout, e);
        }
    }

    private String runCodex(String prompt) {
        List<String> command = properties.getCommand();

        if (command == null || command.isEmpty() || command.stream().anyMatch(part -> part == null || part.isBlank())) {
            throw new IllegalStateException("keel.codex.command must contain non-empty command elements");
        }

        if (properties.getRepoPath() == null || properties.getRepoPath().isBlank()) {
            throw new IllegalStateException("keel.codex.repo-path must not be blank");
        }

        Path repoPath = Path.of(properties.getRepoPath()).toAbsolutePath().normalize();

        if (!Files.isDirectory(repoPath)) {
            throw new IllegalStateException("keel.codex.repo-path must point to an existing directory: " + repoPath);
        }

        try {
            Process process = new ProcessBuilder(command)
                    .directory(repoPath.toFile())
                    .start();

            CompletableFuture<String> stdout = readAsync(process.getInputStream());
            CompletableFuture<String> stderr = readAsync(process.getErrorStream());

            try (var stdin = process.getOutputStream()) {
                stdin.write(prompt.getBytes(StandardCharsets.UTF_8));
            }

            int exitCode = process.waitFor();
            String stdoutText = stdout.get().trim();
            String stderrText = stderr.get().trim();

            if (exitCode != 0) {
                throw new IllegalStateException("""
                        Codex process exited with code %d.
                        stdout: %s
                        stderr: %s
                        """.formatted(exitCode, stdoutText, stderrText).trim());
            }

            return stdoutText;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to run Codex command " + command + " in " + repoPath, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Codex process was interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to read Codex process output", e.getCause());
        }
    }

    private CompletableFuture<String> readAsync(InputStream inputStream) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }

    private String buildPrompt(String requirement) {
        return """
                You are a requirement decomposition engine for a software delivery planning tool.

                Сначала исследуй кодовую базу в своей рабочей директории: прочитай ключевые исходные файлы,
                пойми существующую архитектуру и что уже реализовано. Не отвечай только по тексту требования.

                Сначала агент ОБЯЗАН читать реальный код, как описано выше, но результат разложи
                по трем слоям:
                1. title этапа и description пункта — только человеческий смысл и намерение.
                2. considerations — короткие человеческие предупреждения, риски и подсказки переиспользования.
                3. devNotes — технические детали из репозитория.

                В title этапа и description пункта НЕ ДОЛЖНО быть ни одного имени файла, класса,
                метода, эндпоинта, таблицы, DTO, пакета или другого технического идентификатора.
                Они должны быть понятны не-техническому читателю, например:
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
                Every work item must have a size: S, M, or L.
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
                          "description": "Human-readable work item intent without technical identifiers",
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

                Requirement:
                %s
                """.formatted(requirement);
    }
}
