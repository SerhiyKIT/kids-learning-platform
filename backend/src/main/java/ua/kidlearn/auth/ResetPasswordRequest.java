package ua.kidlearn.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
		@NotBlank String token,
		// Minimum length per docs/Ролі_та_приватність.md §2 (adults).
		@NotBlank @Size(min = 10) String newPassword) {
}
