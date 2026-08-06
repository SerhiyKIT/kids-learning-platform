package ua.kidlearn.aipipeline;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import ua.kidlearn.scenario.ScenarioProblem;

/**
 * Generation parameters for one lesson version. The lesson itself already carries its module
 * (schema fixes module="safety" for the MVP), so no module field here.
 *
 * @param previousProblems validation problems from the prior attempt, fed back in on retry so a
 * real provider can self-correct; null on the first attempt. The stub ignores it.
 */
public record GenerationRequest(@NotBlank String topic, @NotEmpty List<String> patternIds, @NotBlank String ageBand,
		String title, String learningGoal, List<ScenarioProblem> previousProblems) {

	public GenerationRequest withPreviousProblems(List<ScenarioProblem> problems) {
		return new GenerationRequest(topic, patternIds, ageBand, title, learningGoal, problems);
	}

}
