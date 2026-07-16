package ua.kidlearn.attempts;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import ua.kidlearn.auth.AppUserPrincipal;

@RestController
@PreAuthorize("hasRole('TEACHER')")
public class TeacherResultsController {

	private final AttemptService attemptService;

	public TeacherResultsController(AttemptService attemptService) {
		this.attemptService = attemptService;
	}

	@GetMapping("/api/groups/{groupId}/results")
	public List<TeacherResultChild> results(@AuthenticationPrincipal AppUserPrincipal principal,
			@PathVariable UUID groupId) {
		return attemptService.groupResults(principal.getId(), groupId);
	}

}
