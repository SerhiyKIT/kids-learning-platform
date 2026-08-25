package ua.kidlearn.bootstrap;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ua.kidlearn.users.Role;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

/**
 * Creates the very first ADMIN account. See the package javadoc for the three-part safety model:
 * token configured, no ADMIN yet, correct token. That admin lands in the mandatory-2FA
 * "setup required" gate on their first login, same as any other admin — see ua.kidlearn.twofa.
 */
@Service
public class BootstrapService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final BootstrapProperties properties;

	public BootstrapService(UserRepository userRepository, PasswordEncoder passwordEncoder,
			BootstrapProperties properties) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.properties = properties;
	}

	@Transactional
	public User createFirstAdmin(BootstrapAdminRequest request) {
		String configuredToken = properties.adminToken();
		if (configuredToken == null || configuredToken.isBlank()) {
			// Feature off: behave as if the route doesn't exist, same as a dev-only controller
			// under a non-dev profile.
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}

		if (userRepository.existsByRoleAndDeletedAtIsNull(Role.ADMIN)) {
			throw new ResponseStatusException(HttpStatus.GONE, "ALREADY_BOOTSTRAPPED");
		}

		if (!constantTimeEquals(configuredToken, request.token())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		}

		User admin = new User(request.email(), passwordEncoder.encode(request.password()), Role.ADMIN,
				request.displayName(), "uk");
		admin.markEmailVerified();
		try {
			// Flush now so the DB's unique constraint on email fires here, not later — same
			// reasoning as AuthService.register / DevAuthService.
			return userRepository.saveAndFlush(admin);
		} catch (DataIntegrityViolationException e) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered", e);
		}
	}

	private static boolean constantTimeEquals(String expected, String actual) {
		return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
	}

}
