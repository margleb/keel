package dev.keel.controller;

import dev.keel.model.DecompositionRequest;
import dev.keel.requirement.Requirement;
import dev.keel.requirement.RequirementService;
import dev.keel.service.DecompositionService;
import dev.keel.store.DecompositionStorageService;
import dev.keel.store.DecompositionSummary;
import dev.keel.store.StoredDecompositionResponse;
import dev.keel.tracker.TrackerPushResult;
import dev.keel.tracker.TrackerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Разборы")
public class DecompositionController {

    private final DecompositionService decompositionService;
    private final DecompositionStorageService decompositionStorageService;
    private final TrackerService trackerService;
    private final RequirementService requirementService;

    public DecompositionController(
            DecompositionService decompositionService,
            DecompositionStorageService decompositionStorageService,
            TrackerService trackerService,
            RequirementService requirementService
    ) {
        this.decompositionService = decompositionService;
        this.decompositionStorageService = decompositionStorageService;
        this.trackerService = trackerService;
        this.requirementService = requirementService;
    }

    @PostMapping("/decompose")
    @Operation(description = "Запустить разбор требования.")
    public StoredDecompositionResponse decompose(@RequestBody DecompositionRequest request) {
        return decompositionService.decompose(request.requirement(), request.requirementId(), request.projectIds());
    }

    @GetMapping("/decompositions")
    @Operation(description = "Получить последние разборы.")
    public List<DecompositionSummary> listDecompositions() {
        return decompositionStorageService.listRecent();
    }

    @GetMapping("/decompositions/{id}")
    @Operation(description = "Получить разбор по идентификатору.")
    @ApiResponse(responseCode = "404", description = "Разбор не найден.")
    public StoredDecompositionResponse getDecomposition(@PathVariable Long id) {
        return decompositionStorageService.findById(id);
    }

    @PostMapping("/decompositions/{id}/push-to-tracker")
    @Operation(description = "Отправить разбор в трекер.")
    @ApiResponse(responseCode = "404", description = "Разбор не найден.")
    public TrackerPushResult pushToTracker(@PathVariable Long id) {
        TrackerPushResult result = trackerService.pushDecomposition(id);

        StoredDecompositionResponse decomposition = decompositionStorageService.findById(id);
        if (decomposition.requirementId() != null) {
            requirementService.updateStatus(decomposition.requirementId(), Requirement.STATUS_PUSHED);
        }

        return result;
    }
}
