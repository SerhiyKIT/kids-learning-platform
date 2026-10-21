package ua.kidlearn.admin;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ua.kidlearn.audit.AuditAction;
import ua.kidlearn.audit.AuditService;
import ua.kidlearn.audit.AuditTargetType;
import ua.kidlearn.auth.AppUserPrincipal;
import ua.kidlearn.auth.AuthService;
import ua.kidlearn.ratelimit.ClientIpResolver;
import ua.kidlearn.users.User;

/**
 * Invite-based teacher provisioning: the admin supplies only an email and a name; the teacher
 * sets their own password via the emailed link (POST /api/auth/accept-invite). The admin never
 * sees or sets a teacher's password. Every write is audited against the teacher's user id.
 */
@RestController
@RequestMapping("/api/admin/teachers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTeacherController {

	private final AuthService authService;
	private final AdminTeacherService adminTeacherService;
	private final AuditService auditService;
	private final ClientIpResolver clientIpResolver;

	public AdminTeacherController(AuthService authService, AdminTeacherService adminTeacherService,
			AuditService auditService, ClientIpResolver clientIpResolver) {
		this.authService = authService;
		this.adminTeacherService = adminTeacherService;
		this.auditService = auditService;
		this.clientIpResolver = clientIpResolver;
	}

	@GetMapping
	public List<TeacherEntry> list() {
		return adminTeacherService.list();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public InvitedTeacherResponse invite(@Valid @RequestBody InviteTeacherRequest request,
			@AuthenticationPrincipal AppUserPrincipal principal, HttpServletRequest httpRequest) {
		User teacher = authService.inviteTeacher(request.email(), request.displayName());
		audit(principal, AuditAction.INVITE_TEACHER, teacher.getId(), httpRequest);
		return new InvitedTeacherResponse(teacher.getId(), teacher.getEmail(), teacher.getDisplayName(),
				AdminTeacherService.STATUS_INVITED);
	}

	@PostMapping("/{id}/resend-invite")
	public void resendInvite(@PathVariable UUID id, @AuthenticationPrincipal AppUserPrincipal principal,
			HttpServletRequest httpRequest) {
		authService.resendTeacherInvite(id);
		audit(principal, AuditAction.RESEND_TEACHER_INVITE, id, httpRequest);
	}

	@PostMapping("/{id}/deactivate")
	public void deactivate(@PathVariable UUID id, @AuthenticationPrincipal AppUserPrincipal principal,
			HttpServletRequest httpRequest) {
		adminTeacherService.deactivate(id);
		audit(principal, AuditAction.DEACTIVATE_TEACHER, id, httpRequest);
	}

	private void audit(AppUserPrincipal principal, String action, UUID teacherId, HttpServletRequest httpRequest) {
		auditService.record(principal.getId(), principal.getRole(), action, AuditTargetType.USER, teacherId,
				clientIpResolver.resolve(httpRequest));
	}

}
