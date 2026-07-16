package ua.kidlearn.attempts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecordAnswerRequest(
		@NotBlank String sceneKey,
		int tryNo,
		@NotBlank String chosenOption,
		@NotNull Boolean isCorrect,
		short hintsUsed) {
}
