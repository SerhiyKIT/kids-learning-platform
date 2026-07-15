package ua.kidlearn.lessons;

import java.time.Instant;
import java.util.UUID;

public record AssignmentResponse(UUID id, UUID lessonVersionId, UUID groupId, UUID childId, UUID assignedBy,
		Instant availableFrom, Instant dueAt) {

	static AssignmentResponse from(LessonAssignment assignment) {
		return new AssignmentResponse(assignment.getId(), assignment.getLessonVersionId(), assignment.getGroupId(),
				assignment.getChildId(), assignment.getAssignedBy(), assignment.getAvailableFrom(),
				assignment.getDueAt());
	}

}
