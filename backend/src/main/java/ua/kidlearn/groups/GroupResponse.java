package ua.kidlearn.groups;

import java.util.UUID;

public record GroupResponse(UUID id, String name, String joinCode, boolean isActive) {

	static GroupResponse from(Group group) {
		return new GroupResponse(group.getId(), group.getName(), group.getJoinCode(), group.isActive());
	}

}
