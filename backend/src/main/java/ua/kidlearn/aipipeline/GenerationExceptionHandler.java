package ua.kidlearn.aipipeline;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ua.kidlearn.scenario.ScenarioProblem;

@RestControllerAdvice(basePackageClasses = AdminGenerationController.class)
class GenerationExceptionHandler {

	// A refusal is the model declining this request's content, so it's reported as unprocessable;
	// everything else is the upstream provider failing us — a gateway error, not our 500.
	@ExceptionHandler(LlmGenerationException.class)
	ResponseEntity<Map<String, String>> handleLlmGeneration(LlmGenerationException e) {
		HttpStatus status = e.reason() == LlmGenerationException.Reason.REFUSED ? HttpStatus.UNPROCESSABLE_ENTITY
				: HttpStatus.BAD_GATEWAY;
		return ResponseEntity.status(status).body(Map.of("code", "LLM_" + e.reason().name()));
	}

	@ExceptionHandler(InvalidGenerationRequestException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	Map<String, Object> handleInvalidGenerationRequest(InvalidGenerationRequestException e) {
		List<ScenarioProblem> problems = e.problems();
		return Map.of("code", "INVALID_GENERATION_REQUEST", "problems", problems);
	}

}
