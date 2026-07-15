package ua.kidlearn.groups;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ua.kidlearn.auth.AppUserPrincipal;

@RestController
@PreAuthorize("hasRole('PARENT')")
public class GroupMembershipController {

	private final GroupMembershipService groupMembershipService;

	public GroupMembershipController(GroupMembershipService groupMembershipService) {
		this.groupMembershipService = groupMembershipService;
	}

	@PostMapping("/api/groups/join")
	public void join(@AuthenticationPrincipal AppUserPrincipal principal, @Valid @RequestBody JoinGroupRequest request) {
		groupMembershipService.join(principal.getId(), request.joinCode(), request.childId());
	}

	@GetMapping("/api/children/{childId}/groups")
	public List<GroupResponse> groupsForChild(@AuthenticationPrincipal AppUserPrincipal principal,
			@PathVariable UUID childId) {
		return groupMembershipService.listForChild(principal.getId(), childId).stream()
				.map(GroupResponse::from)
				.toList();
	}

	@DeleteMapping("/api/children/{childId}/groups/{groupId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void leave(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID childId,
			@PathVariable UUID groupId) {
		groupMembershipService.leave(principal.getId(), childId, groupId);
	}

}
