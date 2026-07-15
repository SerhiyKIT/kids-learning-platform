package ua.kidlearn.groups;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class GroupMemberId implements Serializable {

	@Column(name = "child_id")
	private UUID childId;

	@Column(name = "group_id")
	private UUID groupId;

	protected GroupMemberId() {
		// JPA
	}

	public GroupMemberId(UUID childId, UUID groupId) {
		this.childId = childId;
		this.groupId = groupId;
	}

	public UUID getChildId() {
		return childId;
	}

	public UUID getGroupId() {
		return groupId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof GroupMemberId that)) {
			return false;
		}
		return Objects.equals(childId, that.childId) && Objects.equals(groupId, that.groupId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(childId, groupId);
	}

}
