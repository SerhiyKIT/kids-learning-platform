package ua.kidlearn.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ua.kidlearn.ratelimit.ClientIpResolver;
import ua.kidlearn.users.User;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;
	private final ReauthenticationService reauthenticationService;
	private final ClientIpResolver clientIpResolver;

	public AuthController(AuthService authService, ReauthenticationService reauthenticationService,
			ClientIpResolver clientIpResolver) {
		this.authService = authService;
		this.reauthenticationService = reauthenticationService;
		this.clientIpResolver = clientIpResolver;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
		try {
			authService.register(request);
		} catch (RegistrationRaceException e) {
			// The address was taken a moment ago — the same answer as for any taken address.
		}
		return RegisterResponse.CHECK_EMAIL;
	}

	@GetMapping("/me")
	public MeResponse me(@AuthenticationPrincipal AppUserPrincipal principal) {
		return new MeResponse(principal.getId(), principal.getUsername(), principal.getRole(),
				principal.isEmailVerified(), principal.getDisplayName());
	}

	@PostMapping("/verify-email")
	public void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
		authService.verifyEmail(request.token());
	}

	@PostMapping("/resend-verification")
	public void resendVerification(@AuthenticationPrincipal AppUserPrincipal principal) {
		authService.resendVerification(principal.getId());
	}

	@PostMapping("/forgot-password")
	public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
		authService.forgotPassword(request.email());
	}

	@PostMapping("/reset-password")
	public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		authService.resetPassword(request.token(), request.newPassword());
	}

	/** Public: an invited teacher sets their password. Doesn't log them in — they sign in next. */
	@PostMapping("/accept-invite")
	public void acceptInvite(@Valid @RequestBody AcceptInviteRequest request) {
		authService.acceptTeacherInvite(request.token(), request.password());
	}

	/**
	 * The signed-in adult re-enters their password (leaving child mode): 204, or 401 with no
	 * detail like a failed login, or 429 once over the login failure limit. The session is unchanged.
	 */
	@PostMapping("/reauthenticate")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void reauthenticate(@AuthenticationPrincipal AppUserPrincipal principal,
			@Valid @RequestBody ReauthenticateRequest request, HttpServletRequest httpRequest) {
		reauthenticationService.reauthenticate(principal.getId(), principal.getUsername(), request.password(),
				clientIpResolver.resolve(httpRequest));
	}

}
