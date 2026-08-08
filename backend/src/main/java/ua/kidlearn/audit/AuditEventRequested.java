package ua.kidlearn.audit;

import java.util.UUID;

record AuditEventRequested(UUID actorId, String actorRole, String action, String targetType, UUID targetId,
		String clientIp) {
}
