package ua.kidlearn.twofa;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * ROLE_ADMIN only — but note these three routes are exactly what {@link TwoFactorGateFilter}
 * keeps reachable for an ADMIN session that hasn't cleared the 2FA gate yet; that's the whole
 * point of them. Takes the plain {@link Principal} (== the authenticated {@code Authentication},
 * Spring resolves it that way) rather than the app's {@code AppUserPrincipal}, deliberately: this
 * package must not depend on {@code ua.kidlearn.auth}, since {@code ua.kidlearn.config} depends
 * on this package and {@code ua.kidlearn.auth} depends on {@code ua.kidlearn.config} — importing
 * it here would close that cycle (see {@code ArchUnitConventionsTest.no_cyclic_dependencies_between_packages}).
 */
@RestController
@RequestMapping("/api/admin/2fa")
@PreAuthorize("hasRole('ADMIN')")
public class TwoFactorController {

	private final TwoFactorService twoFactorService;

	public TwoFactorController(TwoFactorService twoFactorService) {
		this.twoFactorService = twoFactorService;
	}

	@PostMapping("/setup")
	public TwoFactorSetupResponse setup(Principal principal) {
		return twoFactorService.setup(principal.getName());
	}

	@PostMapping("/enable")
	@ResponseStatus(HttpStatus.CREATED)
	public TwoFactorEnableResponse enable(Principal principal, @Valid @RequestBody TwoFactorCodeRequest request,
			HttpServletRequest httpRequest) {
		List<String> backupCodes = twoFactorService.enable(principal.getName(), request.code());
		// Finishing setup already proved a valid code — no reason to also demand /verify.
		TwoFactorSession.elevate(httpRequest);
		return new TwoFactorEnableResponse(backupCodes);
	}

	@PostMapping("/verify")
	public void verify(Principal principal, @Valid @RequestBody TwoFactorCodeRequest request,
			HttpServletRequest httpRequest) {
		if (!twoFactorService.verify(principal.getName(), request.code())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_CODE");
		}
		TwoFactorSession.elevate(httpRequest);
	}

}
