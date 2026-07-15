package ua.kidlearn.lessons;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ua.kidlearn.auth.AppUserPrincipal;

@RestController
@RequestMapping("/api/assignments")
@PreAuthorize("hasRole('TEACHER')")
public class AssignmentController {

	private final AssignmentService assignmentService;

	public AssignmentController(AssignmentService assignmentService) {
		this.assignmentService = assignmentService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AssignmentResponse create(@AuthenticationPrincipal AppUserPrincipal principal,
			@Valid @RequestBody CreateAssignmentRequest request) {
		return AssignmentResponse.from(assignmentService.assign(principal.getId(), request));
	}

	@GetMapping
	public List<AssignmentResponse> list(@AuthenticationPrincipal AppUserPrincipal principal,
			@RequestParam UUID groupId) {
		return assignmentService.listForTeacherAndGroup(principal.getId(), groupId).stream()
				.map(AssignmentResponse::from)
				.toList();
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
		assignmentService.delete(principal.getId(), id);
	}

}
