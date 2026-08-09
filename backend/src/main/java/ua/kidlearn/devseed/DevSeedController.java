package ua.kidlearn.devseed;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.kidlearn.auth.AppUserPrincipal;

/**
 * Development shortcut only: seeds demo content so the scene engine has something to play
 * without walking through admin/teacher setup by hand first. Gated to the "dev" profile via
 * {@link Profile} — the bean (and therefore this route) doesn't exist at all under any other
 * profile. Must be removed before this app ever runs in production.
 */
@RestController
@RequestMapping("/api/dev")
@Profile("dev")
public class DevSeedController {

	private final DevSeedService devSeedService;

	public DevSeedController(DevSeedService devSeedService) {
		this.devSeedService = devSeedService;
	}

	@PostMapping("/seed-demo")
	public DevSeedResponse seedDemo(@AuthenticationPrincipal AppUserPrincipal principal) {
		return devSeedService.seed(principal.getId(), principal.getRole());
	}

}
