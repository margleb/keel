package dev.keel.store;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecompositionRepository extends JpaRepository<Decomposition, Long> {

    List<Decomposition> findTop100ByOrderByCreatedAtDesc();
}
