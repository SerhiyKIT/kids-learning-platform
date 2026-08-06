package ua.kidlearn.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Runs before Spring Security's login processing filter: if the submitted username or the client
 * IP is already over its failed-login limit, short-circuits with 429 instead of attempting
 * authentication (so a locked-out account can't be used to keep probing passwords).
 */
public class LoginThrottleFilter extends OncePerRequestFilter {

	private static final RequestMatcher LOGIN_MATCHER = PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/login");
	private static final String USERNAME_PARAM = "username";

	private final LoginAttemptService loginAttemptService;
	private final ClientIpResolver clientIpResolver;

	public LoginThrottleFilter(LoginAttemptService loginAttemptService, ClientIpResolver clientIpResolver) {
		this.loginAttemptService = loginAttemptService;
		this.clientIpResolver = clientIpResolver;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (LOGIN_MATCHER.matches(request)) {
			String username = request.getParameter(USERNAME_PARAM);
			String clientIp = clientIpResolver.resolve(request);
			if (username != null && loginAttemptService.isBlocked(username, clientIp)) {
				RateLimitResponses.writeTooManyRequests(response, loginAttemptService.retryAfterSeconds(username, clientIp));
				return;
			}
		}
		filterChain.doFilter(request, response);
	}

}
