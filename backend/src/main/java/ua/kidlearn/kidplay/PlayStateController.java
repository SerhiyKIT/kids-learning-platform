package ua.kidlearn.kidplay;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.kidlearn.auth.AppUserPrincipal;

/** Parents of this child only (another parent's child → 404; teachers don't get here). */
@RestController
@RequestMapping("/api/children/{childId}")
@PreAuthorize("hasRole('PARENT')")
public class PlayStateController {

	private final PlayStateService playStateService;

	public PlayStateController(PlayStateService playStateService) {
		this.playStateService = playStateService;
	}

	@GetMapping("/play-state")
	public PlayStateResponse state(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID childId) {
		return playStateService.state(principal.getId(), childId);
	}

	@PutMapping("/play-settings")
	public PlayStateResponse updateSettings(@AuthenticationPrincipal AppUserPrincipal principal,
			@PathVariable UUID childId, @RequestBody PlaySettingsRequest request) {
		return playStateService.updateSettings(principal.getId(), childId, request.sessionLimitMinutes());
	}

	@PostMapping("/play-state/wake")
	public PlayStateResponse wake(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID childId) {
		return playStateService.wake(principal.getId(), childId);
	}

}
