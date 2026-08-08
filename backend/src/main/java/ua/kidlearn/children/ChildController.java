package ua.kidlearn.children;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import ua.kidlearn.ratelimit.ClientIpResolver;

@RestController
@RequestMapping("/api/children")
@PreAuthorize("hasRole('PARENT')")
public class ChildController {

	private final ChildService childService;
	private final AuditService auditService;
	private final ClientIpResolver clientIpResolver;

	public ChildController(ChildService childService, AuditService auditService, ClientIpResolver clientIpResolver) {
		this.childService = childService;
		this.auditService = auditService;
		this.clientIpResolver = clientIpResolver;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ChildResponse create(@AuthenticationPrincipal AppUserPrincipal principal,
			@Valid @RequestBody CreateChildRequest request) {
		return ChildResponse.from(childService.create(principal.getId(), request));
	}

	@PostMapping("/{id}/consent")
	public void consent(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
		childService.grantAccountConsent(principal.getId(), id);
	}

	@GetMapping
	public List<ChildResponse> list(@AuthenticationPrincipal AppUserPrincipal principal) {
		return childService.listForParent(principal.getId()).stream().map(ChildResponse::from).toList();
	}

	@GetMapping("/{id}")
	public ChildResponse get(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
		return ChildResponse.from(childService.getForParent(principal.getId(), id));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id,
			HttpServletRequest request) {
		childService.delete(principal.getId(), id);
		auditService.record(principal.getId(), principal.getRole(), AuditAction.DELETE_CHILD, AuditTargetType.CHILD,
				id, clientIpResolver.resolve(request));
	}

}
