package ua.kidlearn.attempts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Maps exactly to the {@code scene_answers} table (see V2__core_schema.sql). */
@Entity
@Table(name = "scene_answers")
public class SceneAnswer {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "attempt_id", nullable = false)
	private UUID attemptId;

	@Column(name = "scene_key", nullable = false)
	private String sceneKey;

	@Column(name = "try_no", nullable = false)
	private int tryNo;

	@Column(name = "chosen_option", nullable = false)
	private String chosenOption;

	@Column(name = "is_correct", nullable = false)
	private boolean correct;

	@Column(name = "hints_used", nullable = false)
	private short hintsUsed;

	@Column(name = "answered_at", nullable = false)
	private Instant answeredAt;

	protected SceneAnswer() {
		// JPA
	}

	public SceneAnswer(UUID attemptId, String sceneKey, int tryNo, String chosenOption, boolean correct,
			short hintsUsed, Instant answeredAt) {
		this.attemptId = attemptId;
		this.sceneKey = sceneKey;
		this.tryNo = tryNo;
		this.chosenOption = chosenOption;
		this.correct = correct;
		this.hintsUsed = hintsUsed;
		this.answeredAt = answeredAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getAttemptId() {
		return attemptId;
	}

	public String getSceneKey() {
		return sceneKey;
	}

	public int getTryNo() {
		return tryNo;
	}

	public String getChosenOption() {
		return chosenOption;
	}

	public boolean isCorrect() {
		return correct;
	}

	public short getHintsUsed() {
		return hintsUsed;
	}

	public Instant getAnsweredAt() {
		return answeredAt;
	}

}
