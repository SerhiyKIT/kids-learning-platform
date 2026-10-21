package ua.kidlearn.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InviteTeacherRequest(@NotBlank @Email String email, @NotBlank @Size(max = 120) String displayName) {

	/** Strips surrounding whitespace before bean validation sees it (" a@b.c " is a valid email, typed carelessly). */
	public InviteTeacherRequest {
		email = email == null ? null : email.strip();
	}

}
