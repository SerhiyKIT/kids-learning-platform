package ua.kidlearn.audit;

import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.kidlearn.users.Role;

/**
 * Records one data-access/moderation event. Publishes an event rather than writing directly;
 * {@link AuditEventListener} does the actual write asynchronously, after this method's
 * transaction commits, so a slow/broken audit write never slows or fails the request it's
 * recording. {@code @Transactional} here means callers don't need their own active transaction —
 * calling this from a controller, after the underlying action has already succeeded, starts a
 * fresh (near-instant) transaction just for the publish.
 */
@Service
public class AuditService {

	private final ApplicationEventPublisher eventPublisher;
	private final AuditLogRepository auditLogRepository;

	public AuditService(ApplicationEventPublisher eventPublisher, AuditLogRepository auditLogRepository) {
		this.eventPublisher = eventPublisher;
		this.auditLogRepository = auditLogRepository;
	}

	@Transactional
	public void record(UUID actorId, Role actorRole, String action, String targetType, UUID targetId,
			String clientIp) {
		eventPublisher.publishEvent(new AuditEventRequested(actorId, actorRole.name(), action, targetType, targetId,
				clientIp));
	}

	@Transactional(readOnly = true)
	public List<AuditEvent> list(String targetType, UUID targetId, UUID actorId, int limit, int offset) {
		return auditLogRepository.findFiltered(targetType, targetId, actorId, limit, offset);
	}

}
