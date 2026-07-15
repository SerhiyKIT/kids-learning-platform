package ua.kidlearn.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ua.kidlearn.users.User;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
		User user = authService.register(request);
		return new RegisterResponse(user.getId(), user.getEmail(), user.getDisplayName());
	}

	@GetMapping("/me")
	public MeResponse me(@AuthenticationPrincipal AppUserPrincipal principal) {
		return new MeResponse(principal.getId(), principal.getUsername(), principal.getRole());
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

}
