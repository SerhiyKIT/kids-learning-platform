package ua.kidlearn.twofa;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import ua.kidlearn.ratelimit.LoginAttemptService;
import ua.kidlearn.users.SessionAuthentication;
import ua.kidlearn.users.UserRepository;

/**
 * Resets the account's failed-login counter on success (same as the login rate limiter needs
 * regardless of role), then — for ADMIN accounts only — initializes this session's 2FA gate state
 * (see {@link TwoFactorSession}, {@link TwoFactorGateFilter}): SETUP_REQUIRED if they haven't
 * enabled TOTP yet, PENDING_VERIFICATION if they have. Non-admins get no 2FA state at all; the
 * gate filter never restricts them. Looks the user up by email rather than taking the app's
 * {@code AppUserPrincipal} type, deliberately — see {@link TwoFactorController}'s javadoc for why
 * this package must not depend on {@code ua.kidlearn.auth}.
 */
public class TwoFactorSessionAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

	private static final String ROLE_ADMIN = "ROLE_ADMIN";

	private final LoginAttemptService loginAttemptService;
	private final UserRepository userRepository;
	private final Clock clock;

	public TwoFactorSessionAuthenticationSuccessHandler(LoginAttemptService loginAttemptService,
			UserRepository userRepository, Clock clock) {
		this.loginAttemptService = loginAttemptService;
		this.userRepository = userRepository;
		this.clock = clock;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		loginAttemptService.recordSuccess(authentication.getName());
		// Same clock as sessions_valid_after, so "sign out everywhere" compares like with like.
		SessionAuthentication.markAuthenticated(request.getSession(), clock.instant());
		boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> ROLE_ADMIN.equals(a.getAuthority()));
		if (isAdmin) {
			userRepository.findByEmailAndDeletedAtIsNull(authentication.getName())
					.ifPresent(user -> TwoFactorSession.init(request,
							user.isTotpEnabled() ? TwoFactorStatus.PENDING_VERIFICATION : TwoFactorStatus.SETUP_REQUIRED));
		}
		super.onAuthenticationSuccess(request, response, authentication);
	}

}
