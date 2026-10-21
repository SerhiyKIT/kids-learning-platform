package ua.kidlearn.aipipeline;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = AdminVoicingController.class)
class VoicingExceptionHandler {

	// Every reason is the upstream TTS provider failing us — a gateway error, not our 500.
	@ExceptionHandler(TtsSynthesisException.class)
	@ResponseStatus(HttpStatus.BAD_GATEWAY)
	Map<String, String> handleTtsSynthesis(TtsSynthesisException e) {
		return Map.of("code", "TTS_" + e.reason().name());
	}

	@ExceptionHandler(VoicingNotAllowedException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	Map<String, String> handleVoicingNotAllowed() {
		return Map.of("code", "VOICING_REQUIRES_APPROVED");
	}

}
