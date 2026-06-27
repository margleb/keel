package dev.keel.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import java.util.List;

public record DecompositionResult(
        List<Stage> stages,
        @JsonSetter(nulls = Nulls.AS_EMPTY) List<IntegrationRisk> integrationRisks,
        @JsonSetter(nulls = Nulls.AS_EMPTY) List<String> clarifications
) {
    public DecompositionResult {
        integrationRisks = integrationRisks == null ? List.of() : List.copyOf(integrationRisks);
        clarifications = clarifications == null ? List.of() : List.copyOf(clarifications);
    }

    public DecompositionResult(List<Stage> stages, List<IntegrationRisk> integrationRisks) {
        this(stages, integrationRisks, List.of());
    }
}
