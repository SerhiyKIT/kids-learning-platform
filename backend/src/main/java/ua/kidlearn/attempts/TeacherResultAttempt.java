package ua.kidlearn.attempts;

import java.math.BigDecimal;
import java.time.Instant;

public record TeacherResultAttempt(String title, Instant completedAt, String result, BigDecimal score) {
}
