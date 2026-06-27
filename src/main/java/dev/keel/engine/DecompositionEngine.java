package dev.keel.engine;

import dev.keel.model.DecompositionResult;
import java.util.List;

public interface DecompositionEngine {

    DecompositionResult decompose(String requirement, List<Long> projectIds);

    default DecompositionResult decompose(String requirement) {
        return decompose(requirement, List.of());
    }
}
