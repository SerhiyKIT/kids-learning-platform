package ua.kidlearn.groups;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Locale;
import java.util.UUID;

/**
 * A parent types the code from a note or pastes it from a message: surrounding spaces and the
 * case don't matter (codes are generated upper-case; QA1-013).
 */
public record JoinGroupRequest(@NotBlank String joinCode, @NotNull UUID childId) {

	public JoinGroupRequest {
		joinCode = joinCode == null ? null : joinCode.strip().toUpperCase(Locale.ROOT);
	}

}
