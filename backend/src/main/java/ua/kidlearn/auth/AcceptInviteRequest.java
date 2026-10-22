package ua.kidlearn.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ua.kidlearn.users.FitsBcrypt;

public record AcceptInviteRequest(
		@NotBlank String token,
		// Same rule as RegisterRequest/ResetPasswordRequest (docs/Ролі_та_приватність.md §2, adults).
		@NotBlank @Size(min = 10) @FitsBcrypt String password) {
}
