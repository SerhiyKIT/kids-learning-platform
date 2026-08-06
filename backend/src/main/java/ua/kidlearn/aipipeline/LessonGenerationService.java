package ua.kidlearn.aipipeline;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ua.kidlearn.config.LlmProperties;
import ua.kidlearn.lessons.LessonRepository;
import ua.kidlearn.lessons.LessonService;
import ua.kidlearn.lessons.LessonVersion;
import ua.kidlearn.scenario.ScenarioProblem;
import ua.kidlearn.scenario.ScenarioValidator;

/**
 * Generate -> validate -> bounded retry -> persist, per docs/Промпт_генерації_уроків.md §3 and
 * docs/Формат_подання_уроків_та_ШІ.md §7. Reuses the same ScenarioValidator as manual entry
 * (LessonService.createVersion) so structural + business rules are enforced identically either
 * way. Never auto-publishes: the result always lands in the existing human-moderation path.
 */
@Service
public class LessonGenerationService {

	private final LessonRepository lessonRepository;
	private final LessonService lessonService;
	private final LlmProvider llmProvider;
	private final ScenarioValidator scenarioValidator;
	private final int maxAttempts;

	public LessonGenerationService(LessonRepository lessonRepository, LessonService lessonService,
			LlmProvider llmProvider, ScenarioValidator scenarioValidator, LlmProperties llmProperties) {
		this.lessonRepository = lessonRepository;
		this.lessonService = lessonService;
		this.llmProvider = llmProvider;
		this.scenarioValidator = scenarioValidator;
		this.maxAttempts = llmProperties.maxAttempts();
	}

	@Transactional
	public GenerationOutcome generate(UUID lessonId, GenerationRequest request) {
		lessonRepository.findById(lessonId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));

		GenerationRequest currentRequest = request;
		GeneratedScenario generated = null;
		List<ScenarioProblem> problems = List.of();
		int attempts = 0;

		while (attempts < maxAttempts) {
			attempts++;
			generated = llmProvider.generate(currentRequest);
			problems = scenarioValidator.validate(generated.scenario());
			if (problems.isEmpty()) {
				break;
			}
			currentRequest = currentRequest.withPreviousProblems(problems);
		}

		String status = problems.isEmpty() ? LessonVersion.STATUS_AUTO_VALIDATED : LessonVersion.STATUS_REJECTED_AUTO;
		LessonVersion version = lessonService.createGeneratedVersion(lessonId, generated.scenario(), generated.modelId(),
				status);
		return new GenerationOutcome(version.getId(), status, attempts, problems);
	}

}
