package dev.keel.requirement;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequirementRepository extends JpaRepository<Requirement, Long> {

    List<Requirement> findAllByOrderByCreatedAtDesc();
}
