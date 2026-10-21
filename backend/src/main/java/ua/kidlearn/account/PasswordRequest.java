package ua.kidlearn.account;

import jakarta.validation.constraints.NotBlank;

/** DELETE /api/me body: the current password confirms a destructive action. */
public record PasswordRequest(@NotBlank String password) {

	@Override
	public String toString() {
		return "PasswordRequest[password=***]";
	}

}
