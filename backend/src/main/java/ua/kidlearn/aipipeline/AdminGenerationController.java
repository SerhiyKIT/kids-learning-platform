package ua.kidlearn.aipipeline;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.kidlearn.audit.AuditAction;
import ua.kidlearn.audit.AuditService;
import ua.kidlearn.audit.AuditTargetType;
import ua.kidlearn.auth.AppUserPrincipal;
import ua.kidlearn.ratelimit.ClientIpResolver;

/** Part of the internal content pipeline (see the aipipeline package docs). */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminGenerationController {

	private final LessonGenerationService lessonGenerationService;
	private final AuditService auditService;
	private final ClientIpResolver clientIpResolver;

	public AdminGenerationController(LessonGenerationService lessonGenerationService, AuditService auditService,
			ClientIpResolver clientIpResolver) {
		this.lessonGenerationService = lessonGenerationService;
		this.auditService = auditService;
		this.clientIpResolver = clientIpResolver;
	}

	/**
	 * Audited whenever the LLM was actually called: against the created version on success, or
	 * against the lesson when the provider failed and no version exists. A request rejected by
	 * validation (no LLM call) or for an unknown lesson isn't a generation and isn't audited.
	 */
	@PostMapping("/lessons/{id}/generate")
	public GenerationOutcome generate(@PathVariable UUID id, @RequestBody GenerationRequest request,
			@AuthenticationPrincipal AppUserPrincipal principal, HttpServletRequest httpRequest) {
		GenerationOutcome outcome;
		try {
			outcome = lessonGenerationService.generate(id, request);
		} catch (LlmGenerationException e) {
			auditService.record(principal.getId(), principal.getRole(), AuditAction.GENERATE_VERSION,
					AuditTargetType.LESSON, id, clientIpResolver.resolve(httpRequest));
			throw e;
		}
		auditService.record(principal.getId(), principal.getRole(), AuditAction.GENERATE_VERSION,
				AuditTargetType.LESSON_VERSION, outcome.versionId(), clientIpResolver.resolve(httpRequest));
		return outcome;
	}

}
