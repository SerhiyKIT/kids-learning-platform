package ua.kidlearn.attempts;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ua.kidlearn.audit.AuditAction;
import ua.kidlearn.audit.AuditService;
import ua.kidlearn.audit.AuditTargetType;
import ua.kidlearn.auth.AppUserPrincipal;
import ua.kidlearn.ratelimit.ClientIpResolver;

@RestController
@PreAuthorize("hasRole('PARENT')")
public class AttemptController {

	private final AttemptService attemptService;
	private final AuditService auditService;
	private final ClientIpResolver clientIpResolver;

	public AttemptController(AttemptService attemptService, AuditService auditService,
			ClientIpResolver clientIpResolver) {
		this.attemptService = attemptService;
		this.auditService = auditService;
		this.clientIpResolver = clientIpResolver;
	}

	@GetMapping("/api/children/{childId}/available-lessons")
	public List<AvailableLessonEntry> availableLessons(@AuthenticationPrincipal AppUserPrincipal principal,
			@PathVariable UUID childId) {
		return attemptService.availableLessons(principal.getId(), childId);
	}

	@PostMapping("/api/children/{childId}/attempts")
	@ResponseStatus(HttpStatus.CREATED)
	public StartAttemptResponse startAttempt(@AuthenticationPrincipal AppUserPrincipal principal,
			@PathVariable UUID childId, @Valid @RequestBody StartAttemptRequest request) {
		LessonAttempt attempt = attemptService.startAttempt(principal.getId(), childId, request.lessonVersionId());
		return new StartAttemptResponse(attempt.getId());
	}

	@PostMapping("/api/attempts/{attemptId}/answers")
	@ResponseStatus(HttpStatus.CREATED)
	public AnswerResponse recordAnswer(@AuthenticationPrincipal AppUserPrincipal principal,
			@PathVariable UUID attemptId, @Valid @RequestBody RecordAnswerRequest request) {
		SceneAnswer answer = attemptService.recordAnswer(principal.getId(), attemptId, request);
		return new AnswerResponse(answer.getId());
	}

	@PostMapping("/api/attempts/{attemptId}/complete")
	public void complete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID attemptId,
			@Valid @RequestBody CompleteAttemptRequest request) {
		attemptService.complete(principal.getId(), attemptId, request.result(), request.score());
	}

	@GetMapping("/api/children/{childId}/history")
	public List<HistoryEntry> history(@AuthenticationPrincipal AppUserPrincipal principal,
			@PathVariable UUID childId, HttpServletRequest request) {
		List<HistoryEntry> history = attemptService.history(principal.getId(), childId);
		auditService.record(principal.getId(), principal.getRole(), AuditAction.VIEW_CHILD_HISTORY,
				AuditTargetType.CHILD, childId, clientIpResolver.resolve(request));
		return history;
	}

}
