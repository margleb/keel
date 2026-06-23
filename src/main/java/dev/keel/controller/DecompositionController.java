package dev.keel.controller;

import dev.keel.model.DecompositionRequest;
import dev.keel.service.DecompositionService;
import dev.keel.store.DecompositionStorageService;
import dev.keel.store.DecompositionSummary;
import dev.keel.store.StoredDecompositionResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DecompositionController {

    private final DecompositionService decompositionService;
    private final DecompositionStorageService decompositionStorageService;

    public DecompositionController(
            DecompositionService decompositionService,
            DecompositionStorageService decompositionStorageService
    ) {
        this.decompositionService = decompositionService;
        this.decompositionStorageService = decompositionStorageService;
    }

    @PostMapping("/decompose")
    public StoredDecompositionResponse decompose(@RequestBody DecompositionRequest request) {
        return decompositionService.decompose(request.requirement());
    }

    @GetMapping("/decompositions")
    public List<DecompositionSummary> listDecompositions() {
        return decompositionStorageService.listRecent();
    }

    @GetMapping("/decompositions/{id}")
    public StoredDecompositionResponse getDecomposition(@PathVariable Long id) {
        return decompositionStorageService.findById(id);
    }
}
