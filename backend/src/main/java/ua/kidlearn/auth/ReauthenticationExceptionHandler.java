package ua.kidlearn.auth;

import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AuthController.class)
class ReauthenticationExceptionHandler {

	@ExceptionHandler(ReauthenticationFailedException.class)
	ResponseEntity<Map<String, String>> handleFailed() {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("code", "BAD_CREDENTIALS"));
	}

	/** Same shape as the login throttle's 429 (RateLimitResponses): Retry-After + RATE_LIMITED. */
	@ExceptionHandler(ReauthenticationRateLimitedException.class)
	ResponseEntity<Map<String, String>> handleRateLimited(ReauthenticationRateLimitedException e) {
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
				.header(HttpHeaders.RETRY_AFTER, Long.toString(Math.max(e.getRetryAfterSeconds(), 0)))
				.body(Map.of("code", "RATE_LIMITED"));
	}

}
