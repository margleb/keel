package dev.keel.service;

import dev.keel.engine.DecompositionEngine;
import dev.keel.model.DecompositionResult;
import dev.keel.store.Decomposition;
import dev.keel.store.DecompositionStorageService;
import dev.keel.store.StoredDecompositionResponse;
import org.springframework.stereotype.Service;

@Service
public class DecompositionService {

    private final DecompositionEngine decompositionEngine;
    private final DecompositionStorageService decompositionStorageService;

    public DecompositionService(
            DecompositionEngine decompositionEngine,
            DecompositionStorageService decompositionStorageService
    ) {
        this.decompositionEngine = decompositionEngine;
        this.decompositionStorageService = decompositionStorageService;
    }

    public StoredDecompositionResponse decompose(String requirement) {
        DecompositionResult result = decompositionEngine.decompose(requirement);
        Decomposition decomposition = decompositionStorageService.save(requirement, result);

        return decompositionStorageService.toResponse(decomposition, result);
    }
}
