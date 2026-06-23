package dev.keel.store;

import dev.keel.model.DecompositionResult;
import java.time.Instant;

public record StoredDecompositionResponse(
        Long id,
        Instant createdAt,
        String requirement,
        DecompositionResult result
) {
}
