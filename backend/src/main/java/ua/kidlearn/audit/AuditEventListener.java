package ua.kidlearn.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Writes the audit row off the request thread, after AuditService.record's transaction commits. */
@Component
class AuditEventListener {

	private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

	private final AuditLogRepository auditLogRepository;

	AuditEventListener(AuditLogRepository auditLogRepository) {
		this.auditLogRepository = auditLogRepository;
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onAuditEventRequested(AuditEventRequested event) {
		try {
			auditLogRepository.save(new AuditEvent(event.actorId(), event.actorRole(), event.action(),
					event.targetType(), event.targetId(), event.clientIp()));
		} catch (Exception e) {
			log.error("Failed to write audit log entry: action={} targetType={} targetId={}", event.action(),
					event.targetType(), event.targetId(), e);
		}
	}

}
