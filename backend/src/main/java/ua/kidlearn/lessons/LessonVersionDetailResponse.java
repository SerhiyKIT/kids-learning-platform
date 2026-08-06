package ua.kidlearn.lessons;

import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public record LessonVersionDetailResponse(UUID id, UUID lessonId, int versionNo, JsonNode scenario,
		String generatedBy, String aiModel, String status, UUID approvedBy, Instant createdAt) {

	public static LessonVersionDetailResponse from(LessonVersion version, ObjectMapper objectMapper) {
		return new LessonVersionDetailResponse(version.getId(), version.getLessonId(), version.getVersionNo(),
				objectMapper.readTree(version.getScenario()), version.getGeneratedBy(), version.getAiModel(),
				version.getStatus(), version.getApprovedBy(), version.getCreatedAt());
	}

}
