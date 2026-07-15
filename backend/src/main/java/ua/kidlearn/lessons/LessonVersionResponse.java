package ua.kidlearn.lessons;

import java.util.UUID;

public record LessonVersionResponse(UUID id, UUID lessonId, int versionNo, String status) {

	public static LessonVersionResponse from(LessonVersion version) {
		return new LessonVersionResponse(version.getId(), version.getLessonId(), version.getVersionNo(),
				version.getStatus());
	}

}
