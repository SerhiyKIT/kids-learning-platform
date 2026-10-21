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

	/**
	 * Writes the entry inside the CURRENT transaction instead of after commit. For actions that
	 * delete their own actor (account deletion): the after-commit insert would reference a user
	 * row that no longer exists. Written first, the row survives and its actor_id becomes NULL via
	 * ON DELETE SET NULL — the audit trail outlives the account, de-identified.
	 */
	@Transactional
	public void recordWithinTransaction(UUID actorId, Role actorRole, String action, String targetType,
			UUID targetId, String clientIp) {
		auditLogRepository.saveAndFlush(new AuditEvent(actorId, actorRole.name(), action, targetType, targetId,
				clientIp));
	}

	@Transactional(readOnly = true)
	public List<AuditEvent> list(String action, String targetType, UUID targetId, UUID actorId, int limit,
			int offset) {
		return auditLogRepository.findFiltered(action, targetType, targetId, actorId, limit, offset);
	}

}
