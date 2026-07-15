package ua.kidlearn.children;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = ChildController.class)
class ChildExceptionHandler {

	@ExceptionHandler(EmailNotVerifiedException.class)
	@ResponseStatus(HttpStatus.FORBIDDEN)
	Map<String, String> handleEmailNotVerified() {
		return Map.of("code", "EMAIL_NOT_VERIFIED");
	}

}
