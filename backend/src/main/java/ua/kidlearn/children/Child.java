package ua.kidlearn.children;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Maps exactly to the {@code children} table (see V2__core_schema.sql). */
@Entity
@Table(name = "children")
public class Child {

	public static final String STATUS_PENDING_CONSENT = "pending_consent";
	public static final String STATUS_ACTIVE = "active";

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "display_name", nullable = false)
	private String displayName;

	@Column(name = "birth_year", nullable = false)
	private short birthYear;

	@Column(name = "avatar_id", nullable = false)
	private String avatarId;

	@Column(name = "pin_code_hash")
	private String pinCodeHash;

	@Column(name = "created_by", nullable = false)
	private UUID createdBy;

	@Column(nullable = false)
	private String status;

	// DB-owned (DEFAULT now()); Hibernate never writes it.
	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	protected Child() {
		// JPA
	}

	public Child(String displayName, short birthYear, String avatarId, UUID createdBy) {
		this.displayName = displayName;
		this.birthYear = birthYear;
		this.avatarId = avatarId;
		this.createdBy = createdBy;
		this.status = STATUS_PENDING_CONSENT;
	}

	public UUID getId() {
		return id;
	}

	public String getDisplayName() {
		return displayName;
	}

	public short getBirthYear() {
		return birthYear;
	}

	public String getAvatarId() {
		return avatarId;
	}

	public String getPinCodeHash() {
		return pinCodeHash;
	}

	public UUID getCreatedBy() {
		return createdBy;
	}

	public String getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getDeletedAt() {
		return deletedAt;
	}

	public boolean isActive() {
		return STATUS_ACTIVE.equals(status);
	}

	public void activate() {
		this.status = STATUS_ACTIVE;
	}

}
