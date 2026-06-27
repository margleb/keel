package dev.keel.project;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
@Tag(name = "Проекты")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    @Operation(description = "Получить список всех проектов.")
    public List<ProjectSummary> listProjects() {
        return projectService.listAll();
    }

    @GetMapping("/{id}")
    @Operation(description = "Получить проект по идентификатору.")
    public ProjectDetail getProject(@PathVariable Long id) {
        return projectService.findById(id);
    }

    @PostMapping
    @Operation(description = "Создать проект.")
    public ProjectDetail createProject(@RequestBody ProjectRequest request) {
        return projectService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(description = "Обновить проект.")
    public ProjectDetail updateProject(@PathVariable Long id, @RequestBody ProjectRequest request) {
        return projectService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(description = "Удалить проект.")
    public void deleteProject(@PathVariable Long id) {
        projectService.delete(id);
    }
}
