package dev.keel.requirement;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record RequirementUpdateRequest(
        @NotBlank String title,
        @NotBlank String body,
        List<Long> projectIds
) {
}
