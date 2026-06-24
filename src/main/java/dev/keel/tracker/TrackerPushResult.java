package dev.keel.tracker;

import java.util.List;

public record TrackerPushResult(
        int epicsCreated,
        int tasksCreated,
        List<String> epicKeys
) {
}
