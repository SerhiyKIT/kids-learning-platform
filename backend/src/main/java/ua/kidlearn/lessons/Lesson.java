package ua.kidlearn.lessons;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Maps exactly to the {@code lessons} table (see V2__core_schema.sql). */
@Entity
@Table(name = "lessons")
public class Lesson {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "module_id", nullable = false)
	private UUID moduleId;

	@Column(name = "owner_teacher_id")
	private UUID ownerTeacherId;

	@Column(nullable = false)
	private String title;

	@Column(name = "current_version_id")
	private UUID currentVersionId;

	// DB-owned (DEFAULT now()); Hibernate never writes it.
	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	protected Lesson() {
		// JPA
	}

	public Lesson(UUID moduleId, UUID ownerTeacherId, String title) {
		this.moduleId = moduleId;
		this.ownerTeacherId = ownerTeacherId;
		this.title = title;
	}

	public UUID getId() {
		return id;
	}

	public UUID getModuleId() {
		return moduleId;
	}

	public UUID getOwnerTeacherId() {
		return ownerTeacherId;
	}

	public String getTitle() {
		return title;
	}

	public UUID getCurrentVersionId() {
		return currentVersionId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getDeletedAt() {
		return deletedAt;
	}

	public void setCurrentVersionId(UUID currentVersionId) {
		this.currentVersionId = currentVersionId;
	}

}
