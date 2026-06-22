package dev.keel.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import java.util.List;

public record WorkItem(
        String title,
        String description,
        Size size,
        @JsonSetter(nulls = Nulls.AS_EMPTY) List<String> considerations,
        @JsonSetter(nulls = Nulls.AS_EMPTY) List<String> devNotes
) {
    public WorkItem {
        considerations = considerations == null ? List.of() : List.copyOf(considerations);
        devNotes = devNotes == null ? List.of() : List.copyOf(devNotes);
    }

    public WorkItem(String title, String description, Size size) {
        this(title, description, size, List.of(), List.of());
    }
}
