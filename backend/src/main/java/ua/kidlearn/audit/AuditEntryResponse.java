package ua.kidlearn.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditEntryResponse(UUID id, UUID actorId, String actorRole, String action, String targetType,
		UUID targetId, String clientIp, Instant createdAt) {

	public static AuditEntryResponse from(AuditEvent event) {
		return new AuditEntryResponse(event.getId(), event.getActorId(), event.getActorRole(), event.getAction(),
				event.getTargetType(), event.getTargetId(), event.getClientIp(), event.getCreatedAt());
	}

}
