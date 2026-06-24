package dev.keel.model;

public record DecompositionRequest(String requirement, Long requirementId) {

    public DecompositionRequest(String requirement) {
        this(requirement, null);
    }
}
