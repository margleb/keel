package dev.keel.controller;

import dev.keel.model.DecompositionRequest;
import dev.keel.service.DecompositionService;
import dev.keel.store.DecompositionStorageService;
import dev.keel.store.DecompositionSummary;
import dev.keel.store.StoredDecompositionResponse;
import dev.keel.tracker.TrackerPushResult;
import dev.keel.tracker.TrackerService;
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
    private final TrackerService trackerService;

    public DecompositionController(
            DecompositionService decompositionService,
            DecompositionStorageService decompositionStorageService,
            TrackerService trackerService
    ) {
        this.decompositionService = decompositionService;
        this.decompositionStorageService = decompositionStorageService;
        this.trackerService = trackerService;
    }

    @PostMapping("/decompose")
    public StoredDecompositionResponse decompose(@RequestBody DecompositionRequest request) {
        return decompositionService.decompose(request.requirement(), request.requirementId());
    }

    @GetMapping("/decompositions")
    public List<DecompositionSummary> listDecompositions() {
        return decompositionStorageService.listRecent();
    }

    @GetMapping("/decompositions/{id}")
    public StoredDecompositionResponse getDecomposition(@PathVariable Long id) {
        return decompositionStorageService.findById(id);
    }

    @PostMapping("/decompositions/{id}/push-to-tracker")
    public TrackerPushResult pushToTracker(@PathVariable Long id) {
        return trackerService.pushDecomposition(id);
    }
}
