package ua.kidlearn.bootstrap;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ua.kidlearn.users.FitsBcrypt;

public record BootstrapAdminRequest(
		@NotBlank String token,
		@NotBlank @Email String email,
		// Same minimum as RegisterRequest/DevRegisterRoleRequest (docs/Ролі_та_приватність.md §2).
		@NotBlank @Size(min = 10) @FitsBcrypt String password,
		@NotBlank String displayName) {

	/** Surrounding whitespace dropped before bean validation (EmailAddress.normalize does the rest). */
	public BootstrapAdminRequest {
		email = email == null ? null : email.strip();
	}

}
