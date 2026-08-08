package ua.kidlearn.ratelimit;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

/** Resets the account's failed-login counter on success, then defers to the default redirect behavior. */
public class RateLimitingAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

	private final LoginAttemptService loginAttemptService;

	public RateLimitingAuthenticationSuccessHandler(LoginAttemptService loginAttemptService) {
		this.loginAttemptService = loginAttemptService;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		loginAttemptService.recordSuccess(authentication.getName());
		super.onAuthenticationSuccess(request, response, authentication);
	}

}
