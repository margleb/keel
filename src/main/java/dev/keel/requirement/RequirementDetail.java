package dev.keel.requirement;

import java.time.Instant;
import java.util.List;

public record RequirementDetail(
        Long id,
        String title,
        String body,
        String status,
        Instant createdAt,
        Instant updatedAt,
        List<Long> projectIds
) {
}
