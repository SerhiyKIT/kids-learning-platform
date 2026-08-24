package ua.kidlearn.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import ua.kidlearn.ratelimit.RateLimitProperties.Limit;

/**
 * Per-IP request counting (every request counts, not just failures) on the other sensitive auth
 * POSTs — these aren't guarded by the failure-counting login throttle since they don't have a
 * pass/fail outcome the way login does.
 */
public class AuthRateLimitFilter extends OncePerRequestFilter {

	private final RateLimiter rateLimiter;
	private final ClientIpResolver clientIpResolver;
	private final Map<RequestMatcher, Limit> limitsByPath = new LinkedHashMap<>();

	public AuthRateLimitFilter(RateLimiter rateLimiter, ClientIpResolver clientIpResolver,
			RateLimitProperties properties) {
		this.rateLimiter = rateLimiter;
		this.clientIpResolver = clientIpResolver;
		limitsByPath.put(PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/auth/register"),
				properties.registerPerIp());
		limitsByPath.put(PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/auth/forgot-password"),
				properties.forgotPasswordPerIp());
		limitsByPath.put(PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/auth/resend-verification"),
				properties.resendVerificationPerIp());
		limitsByPath.put(PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/auth/verify-email"),
				properties.verifyEmailPerIp());
		limitsByPath.put(PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/auth/reset-password"),
				properties.resetPasswordPerIp());
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		for (Map.Entry<RequestMatcher, Limit> entry : limitsByPath.entrySet()) {
			if (entry.getKey().matches(request)) {
				Limit limit = entry.getValue();
				String key = "authpost:" + request.getRequestURI() + ":" + clientIpResolver.resolve(request);
				if (!rateLimiter.tryAcquire(key, limit.max(), limit.window())) {
					RateLimitResponses.writeTooManyRequests(response, rateLimiter.retryAfterSeconds(key, limit.window()));
					return;
				}
				break;
			}
		}
		filterChain.doFilter(request, response);
	}

}
