package ua.kidlearn.twofa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Maps exactly to the {@code admin_backup_codes} table (see V6__admin_2fa.sql). */
@Entity
@Table(name = "admin_backup_codes")
class AdminBackupCode {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "code_hash", nullable = false)
	private String codeHash;

	@Column(name = "used_at")
	private Instant usedAt;

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	protected AdminBackupCode() {
		// JPA
	}

	AdminBackupCode(UUID userId, String codeHash) {
		this.userId = userId;
		this.codeHash = codeHash;
	}

	UUID getId() {
		return id;
	}

	String getCodeHash() {
		return codeHash;
	}

	boolean isUsed() {
		return usedAt != null;
	}

	void markUsed() {
		this.usedAt = Instant.now();
	}

}
