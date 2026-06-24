package dev.keel.tracker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.keel.model.Stage;
import dev.keel.model.WorkItem;
import dev.keel.store.DecompositionStorageService;
import dev.keel.store.StoredDecompositionResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class TrackerService {

    private static final String TRACKER_API_BASE_URL = "https://api.tracker.yandex.net";

    private final TrackerProperties properties;
    private final DecompositionStorageService decompositionStorageService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public TrackerService(
            TrackerProperties properties,
            DecompositionStorageService decompositionStorageService,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.decompositionStorageService = decompositionStorageService;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(TRACKER_API_BASE_URL)
                .build();
    }

    public TrackerPushResult pushDecomposition(Long decompositionId) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Трекер не настроен");
        }

        StoredDecompositionResponse decomposition = decompositionStorageService.findById(decompositionId);
        int epicsCreated = 0;
        int tasksCreated = 0;
        List<String> epicKeys = new ArrayList<>();

        for (Stage stage : stages(decomposition)) {
            String epicKey = createIssue(epicBody(stage));
            epicsCreated++;
            epicKeys.add(epicKey);

            for (WorkItem workItem : items(stage)) {
                createIssue(taskBody(workItem, epicKey));
                tasksCreated++;
            }
        }

        return new TrackerPushResult(epicsCreated, tasksCreated, epicKeys);
    }

    private String createIssue(Map<String, Object> body) {
        String responseBody = restClient.post()
                .uri("/v2/issues/")
                .headers(this::applyHeaders)
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

        return readIssueKey(responseBody);
    }

    private Map<String, Object> epicBody(Stage stage) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("queue", properties.getQueue());
        body.put("summary", stage.title());
        body.put("type", "task");
        body.put("description", "Этап декомпозиции Keel");
        return body;
    }

    private Map<String, Object> taskBody(WorkItem workItem, String epicKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("queue", properties.getQueue());
        body.put("summary", workItem.title());
        body.put("description", taskDescription(workItem));
        body.put("type", "task");
        body.put("parent", epicKey);
        body.put("tags", List.of(workItem.size().name()));
        return body;
    }

    private String taskDescription(WorkItem workItem) {
        return nullToEmpty(workItem.description())
                + "\n\n**Что учесть:**\n"
                + String.join("\n", considerations(workItem));
    }

    private void applyHeaders(HttpHeaders headers) {
        headers.set(HttpHeaders.AUTHORIZATION, "OAuth " + properties.getToken());
        headers.set(orgHeaderName(), properties.getOrgId());
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    private String orgHeaderName() {
        if ("360".equalsIgnoreCase(properties.getOrgType())) {
            return "X-Org-ID";
        }

        return "X-Cloud-Org-ID";
    }

    private String readIssueKey(String responseBody) {
        try {
            Map<String, Object> response = objectMapper.readValue(responseBody, new TypeReference<>() {
            });
            Object key = response.get("key");

            if (key instanceof String issueKey && !issueKey.isBlank()) {
                return issueKey;
            }

            throw new IllegalStateException("Tracker response does not contain issue key: " + responseBody);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Tracker response JSON is invalid: " + responseBody, e);
        }
    }

    private List<Stage> stages(StoredDecompositionResponse decomposition) {
        if (decomposition.result() == null || decomposition.result().stages() == null) {
            return List.of();
        }

        return decomposition.result().stages();
    }

    private List<WorkItem> items(Stage stage) {
        if (stage.items() == null) {
            return List.of();
        }

        return stage.items();
    }

    private List<String> considerations(WorkItem workItem) {
        if (workItem.considerations() == null) {
            return List.of();
        }

        return workItem.considerations();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
