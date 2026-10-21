package ua.kidlearn.auth;

import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.kidlearn.ratelimit.LoginAttemptService;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

/**
 * Confirms that the adult is still the one holding the device — the password step of leaving
 * child mode (docs/Ролі_та_приватність.md §2). It's a password guess like a login, so it shares
 * LoginAttemptService's failure counters (per account and per IP): an open session must not become
 * a way around the login throttle. The session itself is left untouched either way.
 */
@Service
public class ReauthenticationService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final LoginAttemptService loginAttemptService;

	public ReauthenticationService(UserRepository userRepository, PasswordEncoder passwordEncoder,
			LoginAttemptService loginAttemptService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.loginAttemptService = loginAttemptService;
	}

	@Transactional(readOnly = true)
	public void reauthenticate(UUID userId, String username, String password, String clientIp) {
		if (loginAttemptService.isBlocked(username, clientIp)) {
			throw new ReauthenticationRateLimitedException(loginAttemptService.retryAfterSeconds(username, clientIp));
		}
		boolean matches = userRepository.findById(userId)
				.filter(user -> !user.isDeleted())
				.map(User::getPasswordHash)
				.filter(hash -> passwordEncoder.matches(password, hash))
				.isPresent();
		if (!matches) {
			loginAttemptService.recordFailure(username, clientIp);
			throw new ReauthenticationFailedException();
		}
		loginAttemptService.recordSuccess(username);
	}

}
