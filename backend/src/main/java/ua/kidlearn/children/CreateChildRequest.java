package ua.kidlearn.children;

import jakarta.validation.constraints.NotBlank;

public record CreateChildRequest(
		@NotBlank String displayName,
		int birthYear,
		@NotBlank String relation,
		String avatarId) {
}
