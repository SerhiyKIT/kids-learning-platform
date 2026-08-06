package ua.kidlearn.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param trustForwardedHeader whether to trust X-Forwarded-For for the client IP. MUST only be
 * true behind our own reverse proxy (nginx in prod) — otherwise a client can spoof the header and
 * evade per-IP limits entirely.
 */
@ConfigurationProperties(prefix = "app.ratelimit")
public record RateLimitProperties(boolean trustForwardedHeader, Limit loginPerAccount, Limit loginPerIp,
		Limit forgotPasswordPerIp, Limit resendVerificationPerIp, Limit registerPerIp, Limit verifyEmailPerIp,
		Limit resetPasswordPerIp) {

	public record Limit(int max, Duration window) {
	}

}
