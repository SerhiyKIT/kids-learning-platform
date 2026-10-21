package ua.kidlearn.aipipeline;

/**
 * Voicing was requested for a version that hasn't passed human moderation. Real TTS costs money
 * and unreviewed (or rejected) content must never be voiced, so only approved and published
 * versions qualify. Mapped to 409 by {@link VoicingExceptionHandler}.
 */
public class VoicingNotAllowedException extends RuntimeException {

	public VoicingNotAllowedException(String status) {
		super("Voicing requires an approved or published version, got status " + status);
	}

}
