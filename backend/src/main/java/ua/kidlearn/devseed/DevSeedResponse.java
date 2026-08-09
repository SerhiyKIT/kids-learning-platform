package ua.kidlearn.devseed;

import java.util.UUID;

/** {@code childId} is only set when the caller was an authenticated parent (see PART B). */
public record DevSeedResponse(UUID lessonVersionId, UUID groupId, String joinCode, UUID childId) {
}
