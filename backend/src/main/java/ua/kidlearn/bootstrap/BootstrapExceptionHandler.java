package ua.kidlearn.bootstrap;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * A malformed bootstrap request (e.g. password under 10 chars) is a 422, not Spring MVC's
 * default 400 — this is a one-time, high-stakes request, so it's worth distinguishing "your
 * request body is invalid" from ordinary bad-request noise.
 */
@RestControllerAdvice(basePackageClasses = BootstrapController.class)
class BootstrapExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
	Map<String, String> handleValidation() {
		return Map.of("code", "INVALID_REQUEST");
	}

}
