package ua.kidlearn.children;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ParentChildId implements Serializable {

	@Column(name = "parent_id")
	private UUID parentId;

	@Column(name = "child_id")
	private UUID childId;

	protected ParentChildId() {
		// JPA
	}

	public ParentChildId(UUID parentId, UUID childId) {
		this.parentId = parentId;
		this.childId = childId;
	}

	public UUID getParentId() {
		return parentId;
	}

	public UUID getChildId() {
		return childId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ParentChildId that)) {
			return false;
		}
		return Objects.equals(parentId, that.parentId) && Objects.equals(childId, that.childId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(parentId, childId);
	}

}
