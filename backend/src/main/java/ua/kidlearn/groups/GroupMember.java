package ua.kidlearn.groups;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Maps exactly to the {@code group_members} table (see V2__core_schema.sql). */
@Entity
@Table(name = "group_members")
public class GroupMember {

	@EmbeddedId
	private GroupMemberId id;

	@Column(name = "joined_at", nullable = false)
	private Instant joinedAt;

	protected GroupMember() {
		// JPA
	}

	public GroupMember(UUID childId, UUID groupId, Instant joinedAt) {
		this.id = new GroupMemberId(childId, groupId);
		this.joinedAt = joinedAt;
	}

	public GroupMemberId getId() {
		return id;
	}

	public Instant getJoinedAt() {
		return joinedAt;
	}

}
