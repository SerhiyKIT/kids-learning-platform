package ua.kidlearn.attempts;

import java.time.Instant;
import java.util.UUID;

public record AvailableLessonEntry(UUID lessonVersionId, UUID lessonId, String title, String moduleCode,
		UUID assignmentId, Instant dueAt) {
}
