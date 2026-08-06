package ua.kidlearn.admin;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ua.kidlearn.scenario.ScenarioProblem;
import ua.kidlearn.scenario.ScenarioValidationException;

@RestControllerAdvice(basePackageClasses = AdminLessonController.class)
class AdminExceptionHandler {

	@ExceptionHandler(ScenarioValidationException.class)
	@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
	Map<String, List<ScenarioProblem>> handleScenarioValidation(ScenarioValidationException e) {
		return Map.of("problems", e.problems());
	}

}
