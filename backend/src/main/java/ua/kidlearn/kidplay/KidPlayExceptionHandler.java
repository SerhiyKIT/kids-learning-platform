package ua.kidlearn.kidplay;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Global, like ChildExceptionHandler: the rest check is a PlayStateService rule that surfaces through
// AttemptController (lesson start), outside this package.
@RestControllerAdvice
class KidPlayExceptionHandler {

	@ExceptionHandler(ChildRestingException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	Map<String, Object> handleResting(ChildRestingException e) {
		return Map.of("code", "CHILD_RESTING", "restUntil", e.getRestUntil());
	}

}
