package ua.kidlearn.aipipeline;

/**
 * The LLM provider could not produce a scenario at all (as opposed to producing one that fails
 * ScenarioValidator, which the generation pipeline handles by retry + rejected_auto). Mapped to a
 * clean HTTP response by {@link GenerationExceptionHandler}, never a bare 500. Messages must
 * never contain the API key or the request body.
 */
public class LlmGenerationException extends RuntimeException {

	public enum Reason {
		/** Connect/read timeout or other I/O failure talking to the provider. */
		UNAVAILABLE,
		/** Provider answered with a non-2xx status. */
		UPSTREAM_ERROR,
		/** Model declined the request (stop_reason=refusal). */
		REFUSED,
		/** Output hit max_tokens, so any tool input is truncated. */
		TRUNCATED,
		/** Response had no usable emit_lesson_scenario tool call. */
		NO_SCENARIO
	}

	private final Reason reason;

	public LlmGenerationException(Reason reason, String message) {
		super(message);
		this.reason = reason;
	}

	public LlmGenerationException(Reason reason, String message, Throwable cause) {
		super(message, cause);
		this.reason = reason;
	}

	public Reason reason() {
		return reason;
	}

}
