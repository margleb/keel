package dev.keel.requirement;

import dev.keel.store.DecompositionStorageService;
import dev.keel.store.DecompositionSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/requirements")
public class RequirementController {

    private final RequirementService requirementService;
    private final DecompositionStorageService decompositionStorageService;

    public RequirementController(
            RequirementService requirementService,
            DecompositionStorageService decompositionStorageService
    ) {
        this.requirementService = requirementService;
        this.decompositionStorageService = decompositionStorageService;
    }

    @GetMapping
    public List<RequirementSummary> listRequirements() {
        return requirementService.listAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RequirementDetail createRequirement(@Valid @RequestBody RequirementCreateRequest request) {
        Requirement requirement = requirementService.create(request);

        return requirementService.findById(requirement.getId());
    }

    @GetMapping("/{id}")
    public RequirementDetail getRequirement(@PathVariable Long id) {
        return requirementService.findById(id);
    }

    @PutMapping("/{id}")
    public RequirementDetail updateRequirement(
            @PathVariable Long id,
            @Valid @RequestBody RequirementUpdateRequest request
    ) {
        Requirement requirement = requirementService.update(id, request);

        return requirementService.findById(requirement.getId());
    }

    @GetMapping("/{id}/decompositions")
    public List<DecompositionSummary> listDecompositions(@PathVariable Long id) {
        requirementService.findById(id);

        return decompositionStorageService.listByRequirementId(id);
    }
}
