package ua.kidlearn.attempts;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class AttemptExceptionHandler {

	@ExceptionHandler(AttemptCompletedException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	Map<String, String> handleCompleted() {
		return Map.of("code", "ATTEMPT_COMPLETED");
	}

	@ExceptionHandler(UnknownSceneException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	Map<String, String> handleUnknownScene() {
		return Map.of("code", "UNKNOWN_SCENE");
	}

}
