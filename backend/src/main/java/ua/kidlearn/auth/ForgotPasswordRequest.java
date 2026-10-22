package ua.kidlearn.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import ua.kidlearn.users.EmailAddress;

public record ForgotPasswordRequest(@NotBlank @Email(regexp = EmailAddress.FORMAT) String email) {

	/** Surrounding whitespace dropped before bean validation (EmailAddress.normalize does the rest). */
	public ForgotPasswordRequest {
		email = email == null ? null : email.strip();
	}

}
