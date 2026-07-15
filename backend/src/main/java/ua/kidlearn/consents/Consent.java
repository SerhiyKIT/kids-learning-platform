package ua.kidlearn.consents;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Maps exactly to the {@code consents} table (see V2__core_schema.sql). */
@Entity
@Table(name = "consents")
public class Consent {

	public static final String TYPE_ACCOUNT = "account";

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "child_id", nullable = false)
	private UUID childId;

	@Column(name = "parent_id", nullable = false)
	private UUID parentId;

	@Column(nullable = false)
	private String type;

	@Column(name = "granted_at", nullable = false)
	private Instant grantedAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	protected Consent() {
		// JPA
	}

	public Consent(UUID childId, UUID parentId, String type, Instant grantedAt) {
		this.childId = childId;
		this.parentId = parentId;
		this.type = type;
		this.grantedAt = grantedAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getChildId() {
		return childId;
	}

	public UUID getParentId() {
		return parentId;
	}

	public String getType() {
		return type;
	}

	public Instant getGrantedAt() {
		return grantedAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

}
