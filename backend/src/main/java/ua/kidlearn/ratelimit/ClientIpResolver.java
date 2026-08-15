package ua.kidlearn.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Resolves the client IP used as a rate-limit key. Trusting X-Forwarded-For
 * (app.ratelimit.trust-forwarded-header) is only safe when every request actually passes through
 * our own reverse proxy (nginx in prod), which overwrites/sets that header itself — otherwise a
 * client could put any value there and dodge per-IP limits entirely.
 */
@Component
public class ClientIpResolver {

	private final RateLimitProperties properties;

	public ClientIpResolver(RateLimitProperties properties) {
		this.properties = properties;
	}

	public String resolve(HttpServletRequest request) {
		if (properties.trustForwardedHeader()) {
			String forwardedFor = request.getHeader("X-Forwarded-For");
			if (forwardedFor != null && !forwardedFor.isBlank()) {
				return forwardedFor.split(",")[0].strip();
			}
		}
		return request.getRemoteAddr();
	}

}
