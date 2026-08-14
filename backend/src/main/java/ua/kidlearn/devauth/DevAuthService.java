package ua.kidlearn.devauth;

import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

/**
 * Development shortcut only: the only way to obtain a TEACHER or ADMIN login, since public
 * registration ({@code AuthService.register}) always creates a PARENT by design. Mirrors that
 * method's account-creation mechanics — password hashing via the same {@link PasswordEncoder},
 * duplicate-email mapped to 409 — but honors the requested role and marks the account verified
 * immediately instead of issuing a verification-email token: this exists purely so a developer
 * can reach a role's cabinet, not to model a real signup flow. Gated to the "dev" profile via
 * {@link Profile} — must be removed before this app ever runs in production.
 */
@Service
@Profile("dev")
public class DevAuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public DevAuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public User registerWithRole(DevRegisterRoleRequest request) {
		User user = new User(request.email(), passwordEncoder.encode(request.password()), request.role(),
				request.displayName(), "uk");
		user.markEmailVerified();
		try {
			// Flush now so the DB's unique constraint on email fires here, not later — same
			// reasoning as AuthService.register.
			return userRepository.saveAndFlush(user);
		} catch (DataIntegrityViolationException e) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered", e);
		}
	}

}
