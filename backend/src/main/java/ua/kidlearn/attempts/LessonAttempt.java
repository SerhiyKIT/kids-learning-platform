package ua.kidlearn.attempts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Maps exactly to the {@code lesson_attempts} table (see V2__core_schema.sql). */
@Entity
@Table(name = "lesson_attempts")
public class LessonAttempt {

	public static final String RESULT_COMPLETED = "completed";
	public static final String RESULT_ABANDONED = "abandoned";

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "child_id", nullable = false)
	private UUID childId;

	// null = free playthrough (not used by this feature: starting an attempt
	// always requires a covering assignment, so this is always resolved here).
	@Column(name = "assignment_id")
	private UUID assignmentId;

	@Column(name = "lesson_version_id", nullable = false)
	private UUID lessonVersionId;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column
	private String result;

	@Column
	private BigDecimal score;

	protected LessonAttempt() {
		// JPA
	}

	public LessonAttempt(UUID childId, UUID assignmentId, UUID lessonVersionId, Instant startedAt) {
		this.childId = childId;
		this.assignmentId = assignmentId;
		this.lessonVersionId = lessonVersionId;
		this.startedAt = startedAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getChildId() {
		return childId;
	}

	public UUID getAssignmentId() {
		return assignmentId;
	}

	public UUID getLessonVersionId() {
		return lessonVersionId;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}

	public String getResult() {
		return result;
	}

	public BigDecimal getScore() {
		return score;
	}

	public void complete(String result, BigDecimal score) {
		this.completedAt = Instant.now();
		this.result = result;
		this.score = score;
	}

}
