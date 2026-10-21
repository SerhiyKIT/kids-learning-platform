package ua.kidlearn.themes;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = StyleController.class)
class StyleExceptionHandler {

	@ExceptionHandler(StyleConsentRequiredException.class)
	@ResponseStatus(HttpStatus.FORBIDDEN)
	Map<String, String> handleConsentRequired() {
		return Map.of("code", "STYLE_CONSENT_REQUIRED");
	}

}
