package ua.kidlearn.account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Everything the system keeps about one child, for their parent (GET /api/children/{id}/export).
 * Deliberately excludes other children (group members) and the adults around them (teacher
 * contacts, other parents): groups are names only.
 *
 * @param schemaVersion bumped whenever the shape changes
 */
public record ChildDataExport(int schemaVersion, Instant exportedAt, Profile child, List<ConsentRecord> consents,
		List<GroupRecord> groups, List<AttemptRecord> attempts, PlaySettings playSettings,
		StylePreferences stylePreferences) {

	public static final int SCHEMA_VERSION = 1;

	public record Profile(UUID id, String displayName, int birthYear, String avatarId, String status,
			Instant createdAt) {
	}

	public record ConsentRecord(String type, Instant grantedAt, Instant revokedAt) {
	}

	public record GroupRecord(String name) {
	}

	public record AttemptRecord(UUID attemptId, String lessonTitle, UUID lessonVersionId, Instant startedAt,
			Instant completedAt, String result, BigDecimal score, List<AnswerRecord> answers) {
	}

	public record AnswerRecord(String sceneKey, int tryNo, String chosenOption, boolean isCorrect, int hintsUsed,
			Instant answeredAt) {
	}

	/** @param sessionLimitMinutes null = no limit */
	public record PlaySettings(Integer sessionLimitMinutes) {
	}

	/** Null in the export when there's no style consent or nothing was chosen. */
	public record StylePreferences(String colorId, String worldId, String moodId) {
	}

}
