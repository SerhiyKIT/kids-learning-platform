package ua.kidlearn.attempts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record HistoryEntry(UUID attemptId, String title, Instant startedAt, Instant completedAt, String result,
		BigDecimal score, List<AnswerEntry> answers) {
}
