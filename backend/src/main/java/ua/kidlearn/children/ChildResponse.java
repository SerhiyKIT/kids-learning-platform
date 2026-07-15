package ua.kidlearn.children;

import java.util.UUID;

public record ChildResponse(UUID id, String displayName, short birthYear, String status) {

	static ChildResponse from(Child child) {
		return new ChildResponse(child.getId(), child.getDisplayName(), child.getBirthYear(), child.getStatus());
	}

}
