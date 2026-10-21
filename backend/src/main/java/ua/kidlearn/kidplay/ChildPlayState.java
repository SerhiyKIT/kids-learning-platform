package ua.kidlearn.kidplay;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Maps exactly to the {@code child_play_state} table (see V8__child_play_state.sql). */
@Entity
@Table(name = "child_play_state")
public class ChildPlayState {

	@Id
	@Column(name = "child_id")
	private UUID childId;

	// null = no limit.
	@Column(name = "session_limit_minutes")
	private Short sessionLimitMinutes;

	@Column(name = "session_started_at")
	private Instant sessionStartedAt;

	@Column(name = "last_activity_at")
	private Instant lastActivityAt;

	@Column(name = "rest_until")
	private Instant restUntil;

	protected ChildPlayState() {
		// JPA
	}

	public UUID getChildId() {
		return childId;
	}

	public Short getSessionLimitMinutes() {
		return sessionLimitMinutes;
	}

	public Instant getSessionStartedAt() {
		return sessionStartedAt;
	}

	public Instant getLastActivityAt() {
		return lastActivityAt;
	}

	public Instant getRestUntil() {
		return restUntil;
	}

	public boolean isResting(Instant now) {
		return restUntil != null && now.isBefore(restUntil);
	}

	void setSessionLimitMinutes(Short sessionLimitMinutes) {
		this.sessionLimitMinutes = sessionLimitMinutes;
	}

	void startSession(Instant now) {
		this.sessionStartedAt = now;
		this.restUntil = null;
	}

	void touch(Instant now) {
		this.lastActivityAt = now;
	}

	void rest(Instant until) {
		this.restUntil = until;
		this.sessionStartedAt = null;
	}

	void wake() {
		this.restUntil = null;
		this.sessionStartedAt = null;
	}

}
