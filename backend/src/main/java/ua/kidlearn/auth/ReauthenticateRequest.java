package ua.kidlearn.auth;

import jakarta.validation.constraints.NotBlank;

public record ReauthenticateRequest(@NotBlank String password) {

	// Never let the password reach a log line through a record's generated toString().
	@Override
	public String toString() {
		return "ReauthenticateRequest[password=***]";
	}

}
