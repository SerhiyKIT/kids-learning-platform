package ua.kidlearn.aipipeline;

/**
 * The TTS provider could not synthesize a voice line. Mapped to a clean HTTP response by
 * {@link VoicingExceptionHandler}, never a bare 500. Messages must never contain the API key or
 * the request body.
 */
public class TtsSynthesisException extends RuntimeException {

	public enum Reason {
		/** Connect/read timeout or other I/O failure talking to the provider. */
		UNAVAILABLE,
		/** Provider answered with a non-2xx status or an unreadable body. */
		UPSTREAM_ERROR,
		/** Provider answered 2xx but with no audio. */
		EMPTY_AUDIO
	}

	private final Reason reason;

	public TtsSynthesisException(Reason reason, String message) {
		super(message);
		this.reason = reason;
	}

	public TtsSynthesisException(Reason reason, String message, Throwable cause) {
		super(message, cause);
		this.reason = reason;
	}

	public Reason reason() {
		return reason;
	}

}
