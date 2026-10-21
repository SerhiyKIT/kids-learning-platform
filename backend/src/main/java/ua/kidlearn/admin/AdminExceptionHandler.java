package ua.kidlearn.admin;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ua.kidlearn.auth.EmailTakenException;
import ua.kidlearn.auth.InviteAlreadyAcceptedException;
import ua.kidlearn.lessons.NotApprovedException;
import ua.kidlearn.lessons.NotValidatedException;
import ua.kidlearn.scenario.ScenarioProblem;
import ua.kidlearn.scenario.ScenarioValidationException;

@RestControllerAdvice(basePackageClasses = AdminLessonController.class)
class AdminExceptionHandler {

	@ExceptionHandler(ScenarioValidationException.class)
	@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
	Map<String, List<ScenarioProblem>> handleScenarioValidation(ScenarioValidationException e) {
		return Map.of("problems", e.problems());
	}

	@ExceptionHandler(NotApprovedException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	Map<String, String> handleNotApproved() {
		return Map.of("code", "NOT_APPROVED");
	}

	@ExceptionHandler(NotValidatedException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	Map<String, String> handleNotValidated() {
		return Map.of("code", "NOT_VALIDATED");
	}

	@ExceptionHandler(EmailTakenException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	Map<String, String> handleEmailTaken() {
		return Map.of("code", "EMAIL_TAKEN");
	}

	@ExceptionHandler(InviteAlreadyAcceptedException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	Map<String, String> handleInviteAlreadyAccepted() {
		return Map.of("code", "INVITE_ALREADY_ACCEPTED");
	}

}
