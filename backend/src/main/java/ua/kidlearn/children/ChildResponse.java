package ua.kidlearn.children;

import java.util.UUID;

/**
 * @param hasOtherParent another adult is linked to the child too — removing the profile from this
 * cabinet then only unlinks this parent (QA1-007); never says who the other parent is
 */
public record ChildResponse(UUID id, String displayName, short birthYear, String status, boolean hasOtherParent) {

	static ChildResponse from(Child child, boolean hasOtherParent) {
		return new ChildResponse(child.getId(), child.getDisplayName(), child.getBirthYear(), child.getStatus(),
				hasOtherParent);
	}

}
