package ua.kidlearn.admin;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.kidlearn.audit.AuditEntryResponse;
import ua.kidlearn.audit.AuditLogRepository;

/** Read access to the data-access audit log — admins only (docs/Ролі_та_приватність.md §3, §8). */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditController {

	private final AuditLogRepository auditLogRepository;

	public AdminAuditController(AuditLogRepository auditLogRepository) {
		this.auditLogRepository = auditLogRepository;
	}

	@GetMapping("/audit")
	public List<AuditEntryResponse> list(@RequestParam(required = false) String targetType,
			@RequestParam(required = false) UUID targetId, @RequestParam(required = false) UUID actorId,
			@RequestParam(defaultValue = "50") int limit, @RequestParam(defaultValue = "0") int offset) {
		return auditLogRepository.findFiltered(targetType, targetId, actorId, limit, offset).stream()
				.map(AuditEntryResponse::from)
				.toList();
	}

}
