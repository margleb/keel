package dev.keel.project;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional
    public ProjectDetail create(ProjectRequest request) {
        Project project = new Project();
        project.setName(request.name());
        project.setDescription(request.description());
        return toDetail(projectRepository.save(project));
    }

    @Transactional
    public ProjectDetail update(Long id, ProjectRequest request) {
        Project project = getProject(id);
        project.setName(request.name());
        project.setDescription(request.description());
        return toDetail(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    public ProjectDetail findById(Long id) {
        return toDetail(getProject(id));
    }

    @Transactional(readOnly = true)
    public List<ProjectSummary> listAll() {
        return projectRepository.findAll()
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        Project project = getProject(id);
        projectRepository.delete(project);
    }

    public Project getProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
    }

    private ProjectSummary toSummary(Project project) {
        return new ProjectSummary(project.getId(), project.getName(), project.getCreatedAt());
    }

    private ProjectDetail toDetail(Project project) {
        return new ProjectDetail(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
