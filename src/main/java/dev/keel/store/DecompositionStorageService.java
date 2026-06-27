package dev.keel.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.keel.model.DecompositionResult;
import dev.keel.model.Stage;
import dev.keel.tracker.TrackerPushResult;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DecompositionStorageService {

    private static final int PREVIEW_LENGTH = 120;

    private final DecompositionRepository decompositionRepository;
    private final ObjectMapper objectMapper;

    public DecompositionStorageService(
            DecompositionRepository decompositionRepository,
            ObjectMapper objectMapper
    ) {
        this.decompositionRepository = decompositionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Decomposition save(String requirement, DecompositionResult result) {
        return save(requirement, result, null);
    }

    @Transactional
    public Decomposition save(String requirement, DecompositionResult result, Long requirementId) {
        Decomposition decomposition = new Decomposition();
        decomposition.setRequirement(requirement);
        decomposition.setRequirementId(requirementId);
        decomposition.setResultJson(writeResult(result));
        decomposition.setStageCount(stageCount(result));
        decomposition.setTaskCount(taskCount(result));

        return decompositionRepository.save(decomposition);
    }

    @Transactional(readOnly = true)
    public StoredDecompositionResponse findById(Long id) {
        Decomposition decomposition = decompositionRepository.findById(id)
                .orElseThrow(() -> new DecompositionNotFoundException(id));

        return toResponse(decomposition, readResult(decomposition.getResultJson()));
    }

    @Transactional(readOnly = true)
    public List<DecompositionSummary> listRecent() {
        return decompositionRepository.findTop100ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DecompositionSummary> listByRequirementId(Long requirementId) {
        return decompositionRepository.findByRequirementId(requirementId)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public void saveTrackerResult(Long decompositionId, TrackerPushResult result) {
        Decomposition decomposition = decompositionRepository.findById(decompositionId)
                .orElseThrow(() -> new DecompositionNotFoundException(decompositionId));
        try {
            decomposition.setTrackerEpicKeys(objectMapper.writeValueAsString(result.epicKeys()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize tracker epic keys", e);
        }
        decompositionRepository.save(decomposition);
    }

    @Transactional(readOnly = true)
    public Optional<TrackerPushResult> findTrackerResult(Long decompositionId) {
        Decomposition decomposition = decompositionRepository.findById(decompositionId)
                .orElseThrow(() -> new DecompositionNotFoundException(decompositionId));
        if (decomposition.getTrackerEpicKeys() == null) {
            return Optional.empty();
        }
        try {
            List<String> keys = objectMapper.readValue(
                    decomposition.getTrackerEpicKeys(), new TypeReference<>() {});
            return Optional.of(new TrackerPushResult(keys.size(), decomposition.getTaskCount(), keys));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    public StoredDecompositionResponse toResponse(Decomposition decomposition, DecompositionResult result) {
        return new StoredDecompositionResponse(
                decomposition.getId(),
                decomposition.getCreatedAt(),
                decomposition.getRequirementId(),
                decomposition.getRequirement(),
                result
        );
    }

    private DecompositionSummary toSummary(Decomposition decomposition) {
        return new DecompositionSummary(
                decomposition.getId(),
                decomposition.getCreatedAt(),
                preview(decomposition.getRequirement()),
                decomposition.getStageCount(),
                decomposition.getTaskCount()
        );
    }

    private String writeResult(DecompositionResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize decomposition result", e);
        }
    }

    private DecompositionResult readResult(String resultJson) {
        try {
            return objectMapper.readValue(resultJson, DecompositionResult.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored decomposition result JSON is invalid", e);
        }
    }

    private int stageCount(DecompositionResult result) {
        return result == null || result.stages() == null ? 0 : result.stages().size();
    }

    private int taskCount(DecompositionResult result) {
        if (result == null || result.stages() == null) {
            return 0;
        }

        return result.stages()
                .stream()
                .map(Stage::items)
                .filter(items -> items != null)
                .mapToInt(List::size)
                .sum();
    }

    private String preview(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.replaceAll("\\s+", " ").trim();

        if (normalized.length() <= PREVIEW_LENGTH) {
            return normalized;
        }

        return normalized.substring(0, PREVIEW_LENGTH);
    }
}
