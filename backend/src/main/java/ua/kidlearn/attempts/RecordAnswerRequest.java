package ua.kidlearn.attempts;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * One answer in a scene. Bounded like the child engine produces them (QA1-015): the guide has two
 * hint levels, an option id or a sorting "card:basket" pair is short. The scene itself must be one
 * of the attempt's lesson (AttemptService).
 */
public record RecordAnswerRequest(
		@NotBlank @Size(max = 64) String sceneKey,
		@Min(1) @Max(100) int tryNo,
		@NotBlank @Size(max = 64) String chosenOption,
		@NotNull Boolean isCorrect,
		@Min(0) @Max(2) short hintsUsed) {
}
