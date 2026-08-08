package ua.kidlearn.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Maps to the {@code audit_log} table (see V5__audit_log.sql). Rows are never updated or deleted. */
@Entity
@Table(name = "audit_log")
public class AuditEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "actor_id")
	private UUID actorId;

	@Column(name = "actor_role", nullable = false)
	private String actorRole;

	@Column(nullable = false)
	private String action;

	@Column(name = "target_type", nullable = false)
	private String targetType;

	@Column(name = "target_id")
	private UUID targetId;

	@Column(name = "client_ip")
	private String clientIp;

	// DB-owned (DEFAULT now()); Hibernate never writes it.
	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	protected AuditEvent() {
		// JPA
	}

	public AuditEvent(UUID actorId, String actorRole, String action, String targetType, UUID targetId,
			String clientIp) {
		this.actorId = actorId;
		this.actorRole = actorRole;
		this.action = action;
		this.targetType = targetType;
		this.targetId = targetId;
		this.clientIp = clientIp;
	}

	public UUID getId() {
		return id;
	}

	public UUID getActorId() {
		return actorId;
	}

	public String getActorRole() {
		return actorRole;
	}

	public String getAction() {
		return action;
	}

	public String getTargetType() {
		return targetType;
	}

	public UUID getTargetId() {
		return targetId;
	}

	public String getClientIp() {
		return clientIp;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
