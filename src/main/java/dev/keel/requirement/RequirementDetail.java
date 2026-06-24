package dev.keel.requirement;

import java.time.Instant;

public record RequirementDetail(
        Long id,
        String title,
        String body,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
