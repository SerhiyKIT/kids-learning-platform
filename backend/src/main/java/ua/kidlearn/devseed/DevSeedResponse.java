package ua.kidlearn.devseed;

import java.util.UUID;

/**
 * {@code childId} is only set when the caller was an authenticated parent (see PART B).
 * {@code allScenesLessonVersionId} is a second demo lesson with every scene type (demo, choice,
 * sorting, dialog), also assigned to that child.
 */
public record DevSeedResponse(UUID lessonVersionId, UUID groupId, String joinCode, UUID childId,
		UUID allScenesLessonVersionId) {
}
