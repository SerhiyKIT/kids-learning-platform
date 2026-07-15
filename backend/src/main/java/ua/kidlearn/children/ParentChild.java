package ua.kidlearn.children;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/** Maps exactly to the {@code parent_child} table (see V2__core_schema.sql). */
@Entity
@Table(name = "parent_child")
public class ParentChild {

	@EmbeddedId
	private ParentChildId id;

	@Column(nullable = false)
	private String relation;

	@Column(name = "is_primary", nullable = false)
	private boolean primary;

	protected ParentChild() {
		// JPA
	}

	public ParentChild(UUID parentId, UUID childId, String relation, boolean primary) {
		this.id = new ParentChildId(parentId, childId);
		this.relation = relation;
		this.primary = primary;
	}

	public ParentChildId getId() {
		return id;
	}

	public String getRelation() {
		return relation;
	}

	public boolean isPrimary() {
		return primary;
	}

}
