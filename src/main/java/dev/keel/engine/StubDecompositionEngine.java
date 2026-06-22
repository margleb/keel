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
                                        new WorkItem(
                                                "Identify the main user scenario",
                                                "Clarify who starts the flow, what they need to accomplish, and what result should be visible at the end. Capture the happy path and the main boundaries so the team can agree on the scope.",
                                                Size.S
                                        ),
                                        new WorkItem(
                                                "Define input and validation expectations",
                                                "Describe what information the user must provide and how the system should respond when something is missing or incorrect. The result should make the expected behavior clear for both successful and unsuccessful cases.",
                                                Size.M
                                        )
                                )
                        ),
                        new Stage(
                                "Build service contract",
                                List.of(
                                        new WorkItem(
                                                "Define the public interaction shape",
                                                "Agree what the outside caller sends, what they receive back, and which outcomes must be distinguishable. The result should give implementers and consumers the same understanding of the workflow boundary.",
                                                Size.S
                                        ),
                                        new WorkItem(
                                                "Implement the main workflow behavior",
                                                "Connect the user request to the existing business process and return a clear outcome. The completed task should make the core scenario work end to end with predictable handling of normal and rejected requests.",
                                                Size.M
                                        )
                                )
                        ),
                        new Stage(
                                "Verify integration behavior",
                                List.of(
                                        new WorkItem(
                                                "Verify successful and invalid requests",
                                                "Check that the main scenario produces the expected result and that invalid input is rejected in a useful way. The result should give confidence that callers see stable behavior across the most important cases.",
                                                Size.M
                                        ),
                                        new WorkItem(
                                                "Document rollout assumptions",
                                                "Record the dependencies, operational expectations, and open decisions that must be known before release. The result should help the team understand what still needs confirmation outside the core implementation.",
                                                Size.S
                                        )
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
