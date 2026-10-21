package ua.kidlearn.auth;

/** The account or client IP is over its failed-password limit (shared with login). */
public class ReauthenticationRateLimitedException extends RuntimeException {

	private final long retryAfterSeconds;

	public ReauthenticationRateLimitedException(long retryAfterSeconds) {
		super("Too many attempts");
		this.retryAfterSeconds = retryAfterSeconds;
	}

	public long getRetryAfterSeconds() {
		return retryAfterSeconds;
	}

}
