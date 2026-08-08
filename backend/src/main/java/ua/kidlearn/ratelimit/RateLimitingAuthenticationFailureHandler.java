package ua.kidlearn.ratelimit;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

/** Records a failed-login attempt for the submitted username + client IP, then defers to the default redirect. */
public class RateLimitingAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

	private static final String USERNAME_PARAM = "username";

	private final LoginAttemptService loginAttemptService;
	private final ClientIpResolver clientIpResolver;

	public RateLimitingAuthenticationFailureHandler(LoginAttemptService loginAttemptService,
			ClientIpResolver clientIpResolver) {
		super("/login?error");
		this.loginAttemptService = loginAttemptService;
		this.clientIpResolver = clientIpResolver;
	}

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {
		String username = request.getParameter(USERNAME_PARAM);
		if (username != null) {
			loginAttemptService.recordFailure(username, clientIpResolver.resolve(request));
		}
		super.onAuthenticationFailure(request, response, exception);
	}

}
