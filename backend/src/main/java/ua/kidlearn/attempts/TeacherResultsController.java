package ua.kidlearn.attempts;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import ua.kidlearn.audit.AuditAction;
import ua.kidlearn.audit.AuditService;
import ua.kidlearn.audit.AuditTargetType;
import ua.kidlearn.auth.AppUserPrincipal;
import ua.kidlearn.ratelimit.ClientIpResolver;

@RestController
@PreAuthorize("hasRole('TEACHER')")
public class TeacherResultsController {

	private final AttemptService attemptService;
	private final AuditService auditService;
	private final ClientIpResolver clientIpResolver;

	public TeacherResultsController(AttemptService attemptService, AuditService auditService,
			ClientIpResolver clientIpResolver) {
		this.attemptService = attemptService;
		this.auditService = auditService;
		this.clientIpResolver = clientIpResolver;
	}

	@GetMapping("/api/groups/{groupId}/results")
	public List<TeacherResultChild> results(@AuthenticationPrincipal AppUserPrincipal principal,
			@PathVariable UUID groupId, HttpServletRequest request) {
		List<TeacherResultChild> results = attemptService.groupResults(principal.getId(), groupId);
		auditService.record(principal.getId(), principal.getRole(), AuditAction.VIEW_GROUP_RESULTS,
				AuditTargetType.GROUP, groupId, clientIpResolver.resolve(request));
		return results;
	}

}
