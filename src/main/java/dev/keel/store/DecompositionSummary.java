package dev.keel.store;

import java.time.Instant;

public record DecompositionSummary(
        Long id,
        Instant createdAt,
        String requirementPreview,
        int stageCount,
        int taskCount
) {
}
