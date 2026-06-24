package dev.keel.requirement;

import jakarta.validation.constraints.NotBlank;

public record RequirementCreateRequest(
        @NotBlank String title,
        @NotBlank String body
) {
}
