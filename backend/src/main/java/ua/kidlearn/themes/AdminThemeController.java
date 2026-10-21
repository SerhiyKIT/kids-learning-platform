package ua.kidlearn.themes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.kidlearn.audit.AuditAction;
import ua.kidlearn.audit.AuditService;
import ua.kidlearn.audit.AuditTargetType;
import ua.kidlearn.auth.AppUserPrincipal;
import ua.kidlearn.ratelimit.ClientIpResolver;

/** Theme moderation (ADMIN, behind the 2FA gate like every /api/admin route). Every decision is audited. */
@RestController
@RequestMapping("/api/admin/themes")
@PreAuthorize("hasRole('ADMIN')")
public class AdminThemeController {

	private final AdminThemeService adminThemeService;
	private final AuditService auditService;
	private final ClientIpResolver clientIpResolver;

	public AdminThemeController(AdminThemeService adminThemeService, AuditService auditService,
			ClientIpResolver clientIpResolver) {
		this.adminThemeService = adminThemeService;
		this.auditService = auditService;
		this.clientIpResolver = clientIpResolver;
	}

	@GetMapping
	public List<AdminThemeEntry> list(@RequestParam(defaultValue = Theme.STATUS_DRAFT) String status) {
		return adminThemeService.list(status);
	}

	@GetMapping("/{id}")
	public AdminThemeEntry get(@PathVariable UUID id) {
		return adminThemeService.get(id);
	}

	@PostMapping("/{id}/approve")
	public AdminThemeEntry approve(@PathVariable UUID id, @AuthenticationPrincipal AppUserPrincipal principal,
			HttpServletRequest request) {
		AdminThemeEntry entry = adminThemeService.approve(id, principal.getId());
		audit(principal, AuditAction.APPROVE_THEME, id, request);
		return entry;
	}

	@PostMapping("/{id}/reject")
	public AdminThemeEntry reject(@PathVariable UUID id, @Valid @RequestBody RejectThemeRequest body,
			@AuthenticationPrincipal AppUserPrincipal principal, HttpServletRequest request) {
		AdminThemeEntry entry = adminThemeService.reject(id, body.reason());
		audit(principal, AuditAction.REJECT_THEME, id, request);
		return entry;
	}

	@PostMapping("/regenerate")
	public AdminThemeEntry regenerate(@Valid @RequestBody RegenerateThemeRequest body,
			@AuthenticationPrincipal AppUserPrincipal principal, HttpServletRequest request) {
		AdminThemeEntry entry = adminThemeService.regenerate(body.preferenceKey());
		audit(principal, AuditAction.GENERATE_THEME, entry.id(), request);
		return entry;
	}

	private void audit(AppUserPrincipal principal, String action, UUID themeId, HttpServletRequest request) {
		auditService.record(principal.getId(), principal.getRole(), action, AuditTargetType.THEME, themeId,
				clientIpResolver.resolve(request));
	}

}
