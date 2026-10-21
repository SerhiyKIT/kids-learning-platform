package ua.kidlearn.kidplay;

import java.time.Instant;

/** A lesson start while the child is resting after reaching the session limit. */
public class ChildRestingException extends RuntimeException {

	private final Instant restUntil;

	public ChildRestingException(Instant restUntil) {
		super("Child is resting");
		this.restUntil = restUntil;
	}

	public Instant getRestUntil() {
		return restUntil;
	}

}
