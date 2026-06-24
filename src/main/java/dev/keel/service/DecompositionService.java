package dev.keel.service;

import dev.keel.engine.DecompositionEngine;
import dev.keel.model.DecompositionResult;
import dev.keel.requirement.Requirement;
import dev.keel.requirement.RequirementService;
import dev.keel.store.Decomposition;
import dev.keel.store.DecompositionStorageService;
import dev.keel.store.StoredDecompositionResponse;
import org.springframework.stereotype.Service;

@Service
public class DecompositionService {

    private final DecompositionEngine decompositionEngine;
    private final DecompositionStorageService decompositionStorageService;
    private final RequirementService requirementService;

    public DecompositionService(
            DecompositionEngine decompositionEngine,
            DecompositionStorageService decompositionStorageService,
            RequirementService requirementService
    ) {
        this.decompositionEngine = decompositionEngine;
        this.decompositionStorageService = decompositionStorageService;
        this.requirementService = requirementService;
    }

    public StoredDecompositionResponse decompose(String requirement) {
        return decompose(requirement, null);
    }

    public StoredDecompositionResponse decompose(String requirement, Long requirementId) {
        DecompositionResult result = decompositionEngine.decompose(requirement);
        Decomposition decomposition = decompositionStorageService.save(requirement, result, requirementId);

        if (requirementId != null) {
            requirementService.updateStatus(requirementId, Requirement.STATUS_DECOMPOSED);
        }

        return decompositionStorageService.toResponse(decomposition, result);
    }
}
