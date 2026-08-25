package ua.kidlearn.bootstrap;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ua.kidlearn.users.User;

/**
 * NOT {@code @Profile}-restricted — must be reachable in production, unlike
 * {@code ua.kidlearn.devauth}. See the package javadoc for what makes it safe there anyway.
 */
@RestController
@RequestMapping("/api/bootstrap")
public class BootstrapController {

	private final BootstrapService bootstrapService;

	public BootstrapController(BootstrapService bootstrapService) {
		this.bootstrapService = bootstrapService;
	}

	@PostMapping("/admin")
	@ResponseStatus(HttpStatus.CREATED)
	public BootstrapAdminResponse createFirstAdmin(@Valid @RequestBody BootstrapAdminRequest request) {
		User admin = bootstrapService.createFirstAdmin(request);
		return new BootstrapAdminResponse(admin.getId(), admin.getEmail(), admin.getRole());
	}

}
