package ua.kidlearn.themes;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ua.kidlearn.auth.AppUserPrincipal;

/** Parents of this child only (another parent's child → 404). */
@RestController
@RequestMapping("/api/children/{childId}/style")
@PreAuthorize("hasRole('PARENT')")
public class StyleController {

	private final StyleService styleService;

	public StyleController(StyleService styleService) {
		this.styleService = styleService;
	}

	@GetMapping
	public StyleResponse style(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID childId) {
		return styleService.style(principal.getId(), childId);
	}

	@PutMapping
	public StyleResponse choose(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID childId,
			@Valid @RequestBody StyleAnswers answers) {
		return styleService.choose(principal.getId(), principal.getRole(), childId, answers);
	}

	@PostMapping("/seen")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void seen(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID childId) {
		styleService.markSeen(principal.getId(), childId);
	}

}
