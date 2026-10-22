package ua.kidlearn.children;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateChildRequest(
		// A name or nickname: 1–60 characters (QA1-016).
		@NotBlank @Size(max = 60) String displayName,
		int birthYear,
		@NotBlank String relation,
		String avatarId) {
}
