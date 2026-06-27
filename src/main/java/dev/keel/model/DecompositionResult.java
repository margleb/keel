package dev.keel.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import java.util.List;

public record DecompositionResult(
        List<Stage> stages,
        List<String> integrationRisks,
        @JsonSetter(nulls = Nulls.AS_EMPTY) List<String> clarifications
) {
    public DecompositionResult {
        clarifications = clarifications == null ? List.of() : List.copyOf(clarifications);
    }

    public DecompositionResult(List<Stage> stages, List<String> integrationRisks) {
        this(stages, integrationRisks, List.of());
    }
}
