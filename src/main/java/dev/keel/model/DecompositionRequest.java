package dev.keel.model;

import java.util.List;

public record DecompositionRequest(String requirement, Long requirementId, List<Long> projectIds) {

    public DecompositionRequest(String requirement) {
        this(requirement, null, List.of());
    }

    public DecompositionRequest(String requirement, Long requirementId) {
        this(requirement, requirementId, List.of());
    }
}
