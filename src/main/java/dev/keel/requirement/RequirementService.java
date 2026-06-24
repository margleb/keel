package dev.keel.requirement;

import dev.keel.store.DecompositionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequirementService {

    private final RequirementRepository requirementRepository;
    private final DecompositionRepository decompositionRepository;

    public RequirementService(
            RequirementRepository requirementRepository,
            DecompositionRepository decompositionRepository
    ) {
        this.requirementRepository = requirementRepository;
        this.decompositionRepository = decompositionRepository;
    }

    @Transactional
    public Requirement create(RequirementCreateRequest request) {
        Requirement requirement = new Requirement();
        requirement.setTitle(request.title());
        requirement.setBody(request.body());
        requirement.setStatus(Requirement.STATUS_DRAFT);

        return requirementRepository.save(requirement);
    }

    @Transactional
    public Requirement update(Long id, RequirementUpdateRequest request) {
        Requirement requirement = getRequirement(id);
        requirement.setTitle(request.title());
        requirement.setBody(request.body());

        return requirementRepository.save(requirement);
    }

    @Transactional(readOnly = true)
    public RequirementDetail findById(Long id) {
        return toDetail(getRequirement(id));
    }

    @Transactional(readOnly = true)
    public List<RequirementSummary> listAll() {
        return requirementRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public void updateStatus(Long id, String status) {
        Requirement requirement = getRequirement(id);
        requirement.setStatus(status);
        requirementRepository.save(requirement);
    }

    private Requirement getRequirement(Long id) {
        return requirementRepository.findById(id)
                .orElseThrow(() -> new RequirementNotFoundException(id));
    }

    private RequirementSummary toSummary(Requirement requirement) {
        return new RequirementSummary(
                requirement.getId(),
                requirement.getTitle(),
                requirement.getStatus(),
                requirement.getCreatedAt(),
                Math.toIntExact(decompositionRepository.countByRequirementId(requirement.getId()))
        );
    }

    private RequirementDetail toDetail(Requirement requirement) {
        return new RequirementDetail(
                requirement.getId(),
                requirement.getTitle(),
                requirement.getBody(),
                requirement.getStatus(),
                requirement.getCreatedAt(),
                requirement.getUpdatedAt()
        );
    }
}
