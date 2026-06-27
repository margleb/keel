package dev.keel.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import java.util.List;

public record IntegrationRisk(
        String description,
        String severity,
        @JsonSetter(nulls = Nulls.AS_EMPTY) List<String> affectedStages
) {
    public IntegrationRisk {
        severity = severity == null || severity.isBlank() ? "medium" : severity;
        affectedStages = affectedStages == null ? List.of() : List.copyOf(affectedStages);
    }
}
