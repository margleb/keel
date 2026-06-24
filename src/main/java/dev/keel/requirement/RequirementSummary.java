package dev.keel.requirement;

import java.time.Instant;

public record RequirementSummary(
        Long id,
        String title,
        String status,
        Instant createdAt,
        int decompositionCount
) {
}
