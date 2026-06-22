package dev.keel.model;

import java.util.List;

public record DecompositionResult(List<Stage> stages, List<String> integrationRisks) {
}
