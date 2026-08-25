package ua.kidlearn.bootstrap;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BootstrapAdminRequest(
		@NotBlank String token,
		@NotBlank @Email String email,
		// Same minimum as RegisterRequest/DevRegisterRoleRequest (docs/Ролі_та_приватність.md §2).
		@NotBlank @Size(min = 10) String password,
		@NotBlank String displayName) {
}
