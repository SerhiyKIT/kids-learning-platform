package ua.kidlearn.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
		@NotBlank String currentPassword,
		// Same rule as RegisterRequest (docs/Ролі_та_приватність.md §2, adults).
		@NotBlank @Size(min = 10) String newPassword) {

	@Override
	public String toString() {
		return "ChangePasswordRequest[currentPassword=***, newPassword=***]";
	}

}
