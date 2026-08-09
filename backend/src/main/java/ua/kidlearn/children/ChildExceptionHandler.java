package ua.kidlearn.children;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Deliberately global (no basePackageClasses): EmailNotVerifiedException is a ChildService
// business rule, but it can bubble up through any controller that calls ChildService (e.g. the
// dev-only seed endpoint) — scoping this to ChildController's package let it fall through to a
// bare 500 for callers outside that package. A known business rule must never surface as 500.
@RestControllerAdvice
class ChildExceptionHandler {

	@ExceptionHandler(EmailNotVerifiedException.class)
	@ResponseStatus(HttpStatus.FORBIDDEN)
	Map<String, String> handleEmailNotVerified() {
		return Map.of("code", "EMAIL_NOT_VERIFIED");
	}

}
