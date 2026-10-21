package ua.kidlearn.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Resolves the client IP used as a rate-limit key.
 *
 * <p>With app.ratelimit.trust-forwarded-header (prod, behind nginx) it takes the LAST address of
 * X-Forwarded-For. Each proxy APPENDS the address it was connected from, so only the last entry
 * was written by our own proxy; everything before it is whatever the client sent and can be
 * forged ("1.2.3.4, &lt;real&gt;" must not become a fresh rate-limit key). In prod nginx even overwrites
 * the header with $remote_addr and routes every backend path directly (no second hop), so the
 * header holds exactly one address. Without the setting the header is ignored entirely — the
 * default, since without our proxy in front any value could be spoofed.
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
				String[] chain = forwardedFor.split(",");
				String last = chain[chain.length - 1].strip();
				if (!last.isEmpty()) {
					return last;
				}
			}
		}
		return request.getRemoteAddr();
	}

}
