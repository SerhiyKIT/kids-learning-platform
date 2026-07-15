package ua.kidlearn.groups;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Maps exactly to the {@code groups} table (see V2__core_schema.sql). */
@Entity
@Table(name = "groups")
public class Group {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "teacher_id", nullable = false)
	private UUID teacherId;

	@Column(nullable = false)
	private String name;

	@Column(name = "join_code", nullable = false, unique = true)
	private String joinCode;

	// Named isActive (not active) so Spring Data's derived query
	// findByJoinCodeAndIsActiveTrue resolves against this field directly.
	@Column(name = "is_active", nullable = false)
	private boolean isActive;

	protected Group() {
		// JPA
	}

	public Group(UUID teacherId, String name, String joinCode) {
		this.teacherId = teacherId;
		this.name = name;
		this.joinCode = joinCode;
		this.isActive = true;
	}

	public UUID getId() {
		return id;
	}

	public UUID getTeacherId() {
		return teacherId;
	}

	public String getName() {
		return name;
	}

	public String getJoinCode() {
		return joinCode;
	}

	public boolean isActive() {
		return isActive;
	}

	public void archive() {
		this.isActive = false;
	}

	public void changeJoinCode(String newJoinCode) {
		this.joinCode = newJoinCode;
	}

}
