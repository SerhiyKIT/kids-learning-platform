package ua.kidlearn.lessons;

import java.util.UUID;

public record PendingReviewEntry(UUID versionId, UUID lessonId, String title, int versionNo, String status,
		String generatedBy) {
}
