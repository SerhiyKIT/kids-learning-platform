package ua.kidlearn.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ua.kidlearn.users.EmailAddress;
import ua.kidlearn.users.FitsBcrypt;

public record RegisterRequest(
		@NotBlank @Email(regexp = EmailAddress.FORMAT) String email,
		// Minimum length per docs/Ролі_та_приватність.md §2 (adults).
		@NotBlank @Size(min = 10) @FitsBcrypt String password,
		// An adult's name: 1–60 characters (QA1-011).
		@NotBlank @Size(max = 60) String displayName) {

	/** Surrounding whitespace dropped before bean validation (EmailAddress.normalize does the rest). */
	public RegisterRequest {
		email = email == null ? null : email.strip();
	}

}
