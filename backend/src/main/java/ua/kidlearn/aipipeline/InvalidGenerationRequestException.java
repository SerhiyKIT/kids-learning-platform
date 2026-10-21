package ua.kidlearn.aipipeline;

import java.util.List;
import ua.kidlearn.scenario.ScenarioProblem;

/**
 * A GenerationRequest that doesn't fit the scenario vocabulary (topic, age_band, pattern_ids) —
 * rejected before any LLM call. Mapped to 400 with the per-field problems by
 * {@link GenerationExceptionHandler}.
 */
public class InvalidGenerationRequestException extends RuntimeException {

	private final List<ScenarioProblem> problems;

	public InvalidGenerationRequestException(List<ScenarioProblem> problems) {
		super("Generation request failed validation with " + problems.size() + " problem(s)");
		this.problems = List.copyOf(problems);
	}

	public List<ScenarioProblem> problems() {
		return problems;
	}

}
