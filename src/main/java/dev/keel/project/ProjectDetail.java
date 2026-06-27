package dev.keel.project;

import java.time.Instant;

public record ProjectDetail(Long id, String name, String description, Instant createdAt, Instant updatedAt) {
}
