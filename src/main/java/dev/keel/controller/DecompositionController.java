package dev.keel.controller;

import dev.keel.model.DecompositionRequest;
import dev.keel.model.DecompositionResult;
import dev.keel.service.DecompositionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DecompositionController {

    private final DecompositionService decompositionService;

    public DecompositionController(DecompositionService decompositionService) {
        this.decompositionService = decompositionService;
    }

    @PostMapping("/decompose")
    public DecompositionResult decompose(@RequestBody DecompositionRequest request) {
        return decompositionService.decompose(request.requirement());
    }
}
