package ua.kidlearn.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Minimal endpoint used as a method-security smoke test (ROLE_ADMIN only). */
@RestController
public class AdminPingController {

	@GetMapping("/api/admin/ping")
	@PreAuthorize("hasRole('ADMIN')")
	public String ping() {
		return "pong";
	}

}
