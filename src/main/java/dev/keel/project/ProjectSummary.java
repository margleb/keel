package dev.keel.project;

import java.time.Instant;

public record ProjectSummary(Long id, String name, Instant createdAt) {
}
