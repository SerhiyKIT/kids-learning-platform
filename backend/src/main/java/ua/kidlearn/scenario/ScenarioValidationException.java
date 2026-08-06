package ua.kidlearn.scenario;

import java.util.List;

/** Thrown by callers of {@link ScenarioValidator} when a scenario fails validation. */
public class ScenarioValidationException extends RuntimeException {

	private final List<ScenarioProblem> problems;

	public ScenarioValidationException(List<ScenarioProblem> problems) {
		super("Scenario failed validation with " + problems.size() + " problem(s)");
		this.problems = problems;
	}

	public List<ScenarioProblem> problems() {
		return problems;
	}

}
