package dev.keel.engine;

import dev.keel.model.DecompositionResult;
import dev.keel.model.Size;
import dev.keel.model.Stage;
import dev.keel.model.WorkItem;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "keel.engine", havingValue = "stub", matchIfMissing = true)
public class StubDecompositionEngine implements DecompositionEngine {

    @Override
    public DecompositionResult decompose(String requirement) {
        return new DecompositionResult(
                List.of(
                        new Stage(
                                "Clarify business flow",
                                List.of(
                                        new WorkItem("Identify the main user scenario and expected outcome", Size.S),
                                        new WorkItem("Define input data, validation rules, and error cases", Size.M)
                                )
                        ),
                        new Stage(
                                "Build service contract",
                                List.of(
                                        new WorkItem("Design request and response DTOs for the public API", Size.S),
                                        new WorkItem("Implement the endpoint and service orchestration", Size.M)
                                )
                        ),
                        new Stage(
                                "Verify integration behavior",
                                List.of(
                                        new WorkItem("Add API-level tests for successful and invalid requests", Size.M),
                                        new WorkItem("Document expected dependencies and rollout assumptions", Size.S)
                                )
                        )
                ),
                List.of(
                        "External systems may interpret validation errors differently from this service.",
                        "The final workflow can require additional authorization checks before production rollout."
                )
        );
    }
}
