package ua.kidlearn.attempts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** @param scenes per-scene outcome, in the order the child played them (QA1-031) */
public record TeacherResultAttempt(String title, Instant completedAt, String result, BigDecimal score,
		List<TeacherSceneResult> scenes) {
}
