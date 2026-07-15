package ua.kidlearn.lessons;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Maps exactly to the {@code lesson_assignments} table (see V2__core_schema.sql). */
@Entity
@Table(name = "lesson_assignments")
public class LessonAssignment {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "lesson_version_id", nullable = false)
	private UUID lessonVersionId;

	@Column(name = "group_id")
	private UUID groupId;

	@Column(name = "child_id")
	private UUID childId;

	@Column(name = "assigned_by", nullable = false)
	private UUID assignedBy;

	@Column(name = "available_from")
	private Instant availableFrom;

	@Column(name = "due_at")
	private Instant dueAt;

	protected LessonAssignment() {
		// JPA
	}

	public LessonAssignment(UUID lessonVersionId, UUID groupId, UUID childId, UUID assignedBy,
			Instant availableFrom, Instant dueAt) {
		this.lessonVersionId = lessonVersionId;
		this.groupId = groupId;
		this.childId = childId;
		this.assignedBy = assignedBy;
		this.availableFrom = availableFrom;
		this.dueAt = dueAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getLessonVersionId() {
		return lessonVersionId;
	}

	public UUID getGroupId() {
		return groupId;
	}

	public UUID getChildId() {
		return childId;
	}

	public UUID getAssignedBy() {
		return assignedBy;
	}

	public Instant getAvailableFrom() {
		return availableFrom;
	}

	public Instant getDueAt() {
		return dueAt;
	}

}
