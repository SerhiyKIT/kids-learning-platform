package ua.kidlearn.account;

import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ua.kidlearn.auth.ReauthenticationFailedException;
import ua.kidlearn.auth.ReauthenticationRateLimitedException;

/** Same responses as POST /api/auth/reauthenticate for the password checks behind account actions. */
@RestControllerAdvice(assignableTypes = AccountController.class)
class AccountExceptionHandler {

	@ExceptionHandler(ReauthenticationFailedException.class)
	ResponseEntity<Map<String, String>> handleWrongPassword() {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("code", "BAD_CREDENTIALS"));
	}

	@ExceptionHandler(ReauthenticationRateLimitedException.class)
	ResponseEntity<Map<String, String>> handleRateLimited(ReauthenticationRateLimitedException e) {
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
				.header(HttpHeaders.RETRY_AFTER, Long.toString(Math.max(e.getRetryAfterSeconds(), 0)))
				.body(Map.of("code", "RATE_LIMITED"));
	}

	@ExceptionHandler(AccountDeletionNotAllowedException.class)
	ResponseEntity<Map<String, String>> handleNotAllowed(AccountDeletionNotAllowedException e) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("code", e.getCode(), "message", e.getMessage()));
	}

}
