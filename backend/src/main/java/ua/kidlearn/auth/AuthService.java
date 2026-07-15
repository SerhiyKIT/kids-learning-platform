package ua.kidlearn.auth;

import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ua.kidlearn.config.AuthProperties;
import ua.kidlearn.users.Role;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final UserTokenRepository userTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final TokenGenerator tokenGenerator;
	private final AuthProperties authProperties;
	private final ApplicationEventPublisher eventPublisher;

	public AuthService(UserRepository userRepository, UserTokenRepository userTokenRepository,
			PasswordEncoder passwordEncoder, TokenGenerator tokenGenerator, AuthProperties authProperties,
			ApplicationEventPublisher eventPublisher) {
		this.userRepository = userRepository;
		this.userTokenRepository = userTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenGenerator = tokenGenerator;
		this.authProperties = authProperties;
		this.eventPublisher = eventPublisher;
	}

	@Transactional
	public User register(RegisterRequest request) {
		User user = new User(request.email(), passwordEncoder.encode(request.password()), Role.PARENT,
				request.displayName(), "uk");
		try {
			// Flush now so the DB's unique constraint on email fires here, not later.
			user = userRepository.saveAndFlush(user);
		} catch (DataIntegrityViolationException e) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered", e);
		}
		issueEmailVerificationToken(user);
		return user;
	}

	@Transactional
	public void verifyEmail(String rawToken) {
		UserToken token = userTokenRepository.findByTokenHash(tokenGenerator.hash(rawToken))
				.filter(t -> t.getType() == TokenType.EMAIL_VERIFICATION)
				.filter(t -> t.isUsable(Instant.now()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired token"));
		token.getUser().markEmailVerified();
		token.markUsed();
	}

	@Transactional
	public void resendVerification(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + userId));
		if (user.isEmailVerified()) {
			return;
		}
		if (!recentTokenIssued(user, TokenType.EMAIL_VERIFICATION)) {
			issueEmailVerificationToken(user);
		}
	}

	@Transactional
	public void forgotPassword(String email) {
		userRepository.findByEmailAndDeletedAtIsNull(email).ifPresent(user -> {
			if (!recentTokenIssued(user, TokenType.PASSWORD_RESET)) {
				issuePasswordResetToken(user);
			}
		});
	}

	@Transactional
	public void resetPassword(String rawToken, String newPassword) {
		UserToken token = userTokenRepository.findByTokenHash(tokenGenerator.hash(rawToken))
				.filter(t -> t.getType() == TokenType.PASSWORD_RESET)
				.filter(t -> t.isUsable(Instant.now()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired token"));
		token.getUser().changePassword(passwordEncoder.encode(newPassword));
		token.markUsed();
		// TODO: invalidate this user's active sessions (needs a session registry).
	}

	private void issueEmailVerificationToken(User user) {
		String rawToken = tokenGenerator.generateRawToken();
		Instant expiresAt = Instant.now().plus(authProperties.emailVerificationTtl());
		userTokenRepository.save(new UserToken(user, TokenType.EMAIL_VERIFICATION, tokenGenerator.hash(rawToken), expiresAt));
		eventPublisher.publishEvent(new EmailVerificationRequestedEvent(user, rawToken));
	}

	private void issuePasswordResetToken(User user) {
		String rawToken = tokenGenerator.generateRawToken();
		Instant expiresAt = Instant.now().plus(authProperties.passwordResetTtl());
		userTokenRepository.save(new UserToken(user, TokenType.PASSWORD_RESET, tokenGenerator.hash(rawToken), expiresAt));
		eventPublisher.publishEvent(new PasswordResetRequestedEvent(user, rawToken));
	}

	private boolean recentTokenIssued(User user, TokenType type) {
		return userTokenRepository.findFirstByUserAndTypeOrderByCreatedAtDesc(user, type)
				.map(t -> t.getCreatedAt().isAfter(Instant.now().minusSeconds(60)))
				.orElse(false);
	}

}
