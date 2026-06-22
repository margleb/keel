package dev.keel.service;

import dev.keel.engine.DecompositionEngine;
import dev.keel.model.DecompositionResult;
import org.springframework.stereotype.Service;

@Service
public class DecompositionService {

    private final DecompositionEngine decompositionEngine;

    public DecompositionService(DecompositionEngine decompositionEngine) {
        this.decompositionEngine = decompositionEngine;
    }

    public DecompositionResult decompose(String requirement) {
        return decompositionEngine.decompose(requirement);
    }
}
