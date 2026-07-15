package ua.kidlearn.lessons;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CreateAssignmentRequest(
		@NotNull UUID lessonVersionId,
		UUID groupId,
		UUID childId,
		Instant availableFrom,
		Instant dueAt) {
}
