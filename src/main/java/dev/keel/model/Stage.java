package dev.keel.model;

import java.util.List;

public record Stage(String title, List<WorkItem> items) {
}
