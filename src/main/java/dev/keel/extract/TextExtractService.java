package dev.keel.extract;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.keel.engine.LlmProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TextExtractService {

    private static final String ANTHROPIC_DEFAULT_BASE_URL = "https://api.anthropic.com";
    private static final String ANTHROPIC_MESSAGES_PATH = "/v1/messages";
    private static final String EXTRACT_PROMPT = """
            Извлеки полный текст требования с этого скриншота. Верни только текст требования без пояснений, форматирования и комментариев. Сохрани структуру: заголовки, списки, описания.
            """;

    private final LlmProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public TextExtractService(LlmProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
    }

    public String extractText(MultipartFile image) {
        String mediaType = requireNonBlank(image.getContentType(), "image content type")
                .toLowerCase(Locale.ROOT);
        String data = base64(image);

        Map<String, Object> source = new LinkedHashMap<>();
        source.put("type", "base64");
        source.put("media_type", mediaType);
        source.put("data", data);

        Map<String, Object> imageContent = new LinkedHashMap<>();
        imageContent.put("type", "image");
        imageContent.put("source", source);

        Map<String, Object> textContent = new LinkedHashMap<>();
        textContent.put("type", "text");
        textContent.put("text", EXTRACT_PROMPT.strip());

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", List.of(imageContent, textContent));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", requireNonBlank(properties.getModel(), "keel.llm.model"));
        body.put("max_tokens", 4096);
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

    private String base64(MultipartFile image) {
        try {
            return Base64.getEncoder().encodeToString(image.getBytes());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded image", e);
        }
    }

    private void applyAnthropicHeaders(HttpHeaders headers, String apiKey) {
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");
        headers.setContentType(MediaType.APPLICATION_JSON);
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

    private String requireNonBlank(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " must not be blank");
        }

        return value.trim();
    }
}
