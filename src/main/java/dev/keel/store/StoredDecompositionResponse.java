package dev.keel.store;

import dev.keel.model.DecompositionResult;
import java.time.Instant;

public record StoredDecompositionResponse(
        Long id,
        Instant createdAt,
        Long requirementId,
        String requirement,
        DecompositionResult result
) {
}
