package dev.keel.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import java.util.List;

public record WorkItem(
        String title,
        String description,
        Size size,
        @JsonSetter(nulls = Nulls.AS_EMPTY) List<String> considerations,
        @JsonSetter(nulls = Nulls.AS_EMPTY) List<String> acceptanceCriteria,
        @JsonSetter(nulls = Nulls.AS_EMPTY) List<String> devNotes
) {
    public WorkItem {
        considerations = considerations == null ? List.of() : List.copyOf(considerations);
        acceptanceCriteria = acceptanceCriteria == null ? List.of() : List.copyOf(acceptanceCriteria);
        devNotes = devNotes == null ? List.of() : List.copyOf(devNotes);
    }

    public WorkItem(String title, String description, Size size, List<String> considerations, List<String> devNotes) {
        this(title, description, size, considerations, List.of(), devNotes);
    }

    public WorkItem(String title, String description, Size size) {
        this(title, description, size, List.of(), List.of(), List.of());
    }
}
