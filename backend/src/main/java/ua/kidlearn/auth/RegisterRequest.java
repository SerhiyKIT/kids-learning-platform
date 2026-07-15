package ua.kidlearn.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank @Email String email,
		// Minimum length per docs/Ролі_та_приватність.md §2 (adults).
		@NotBlank @Size(min = 10) String password,
		@NotBlank String displayName) {
}
