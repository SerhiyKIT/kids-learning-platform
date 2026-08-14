package ua.kidlearn.devauth;

import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ua.kidlearn.users.User;

/**
 * Development shortcut only: see {@link DevAuthService}'s javadoc. Gated to the "dev" profile via
 * {@link Profile} — the bean (and therefore this route) doesn't exist at all under any other
 * profile. Must be removed before this app ever runs in production.
 */
@RestController
@RequestMapping("/api/dev")
@Profile("dev")
public class DevAuthController {

	private final DevAuthService devAuthService;

	public DevAuthController(DevAuthService devAuthService) {
		this.devAuthService = devAuthService;
	}

	@PostMapping("/register-role")
	@ResponseStatus(HttpStatus.CREATED)
	public DevRegisterRoleResponse registerRole(@Valid @RequestBody DevRegisterRoleRequest request) {
		User user = devAuthService.registerWithRole(request);
		return new DevRegisterRoleResponse(user.getId(), user.getEmail(), user.getRole());
	}

}
