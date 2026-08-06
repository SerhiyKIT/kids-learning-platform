package ua.kidlearn.aipipeline;

import java.util.List;
import java.util.UUID;
import ua.kidlearn.scenario.ScenarioProblem;

/** Result of one generation pipeline run: the persisted version, its final status, how many
 * LLM attempts it took, and the validation problems of the last attempt (empty when auto_validated). */
public record GenerationOutcome(UUID versionId, String status, int attempts, List<ScenarioProblem> problems) {
}
