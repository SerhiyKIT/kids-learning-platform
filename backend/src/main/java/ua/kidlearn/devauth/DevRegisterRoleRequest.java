package ua.kidlearn.devauth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ua.kidlearn.users.EmailAddress;
import ua.kidlearn.users.FitsBcrypt;
import ua.kidlearn.users.Role;

public record DevRegisterRoleRequest(
		@NotBlank @Email(regexp = EmailAddress.FORMAT) String email,
		// Same minimum as RegisterRequest (docs/Ролі_та_приватність.md §2).
		@NotBlank @Size(min = 10) @FitsBcrypt String password,
		@NotBlank String displayName,
		@NotNull Role role) {

	/** Surrounding whitespace dropped before bean validation (EmailAddress.normalize does the rest). */
	public DevRegisterRoleRequest {
		email = email == null ? null : email.strip();
	}

}
