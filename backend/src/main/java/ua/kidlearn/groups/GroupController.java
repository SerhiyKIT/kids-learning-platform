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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ua.kidlearn.auth.AppUserPrincipal;

@RestController
@RequestMapping("/api/groups")
@PreAuthorize("hasRole('TEACHER')")
public class GroupController {

	private final GroupService groupService;

	public GroupController(GroupService groupService) {
		this.groupService = groupService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public GroupResponse create(@AuthenticationPrincipal AppUserPrincipal principal,
			@Valid @RequestBody CreateGroupRequest request) {
		return GroupResponse.from(groupService.create(principal.getId(), request.name()));
	}

	@GetMapping
	public List<GroupResponse> list(@AuthenticationPrincipal AppUserPrincipal principal) {
		return groupService.listForTeacher(principal.getId()).stream().map(GroupResponse::from).toList();
	}

	@PostMapping("/{id}/archive")
	public void archive(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
		groupService.archive(principal.getId(), id);
	}

	@PostMapping("/{id}/regenerate-code")
	public GroupResponse regenerateCode(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
		return GroupResponse.from(groupService.regenerateCode(principal.getId(), id));
	}

	@GetMapping("/{id}/members")
	public List<GroupMemberInfo> members(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
		return groupService.listMembers(principal.getId(), id);
	}

	@DeleteMapping("/{id}/members/{childId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removeMember(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id,
			@PathVariable UUID childId) {
		groupService.removeMember(principal.getId(), id, childId);
	}

}
