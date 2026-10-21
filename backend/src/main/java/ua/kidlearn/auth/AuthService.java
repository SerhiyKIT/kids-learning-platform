package ua.kidlearn.auth;

import java.time.Clock;
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
import ua.kidlearn.users.EmailAddress;
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
	private final Clock clock;

	public AuthService(UserRepository userRepository, UserTokenRepository userTokenRepository,
			PasswordEncoder passwordEncoder, TokenGenerator tokenGenerator, AuthProperties authProperties,
			ApplicationEventPublisher eventPublisher, Clock clock) {
		this.userRepository = userRepository;
		this.userTokenRepository = userTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenGenerator = tokenGenerator;
		this.authProperties = authProperties;
		this.eventPublisher = eventPublisher;
		this.clock = clock;
	}

	@Transactional
	public User register(RegisterRequest request) {
		if (userRepository.existsAnyByEmail(request.email())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
		}
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
				.filter(t -> t.isUsable(clock.instant()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired token"));
		token.getUser().markEmailVerified(clock.instant());
		token.markUsed(clock.instant());
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
		userRepository.findActiveByEmail(email).ifPresent(user -> {
			if (!recentTokenIssued(user, TokenType.PASSWORD_RESET)) {
				issuePasswordResetToken(user);
			}
		});
	}

	@Transactional
	public void resetPassword(String rawToken, String newPassword) {
		UserToken token = userTokenRepository.findByTokenHash(tokenGenerator.hash(rawToken))
				.filter(t -> t.getType() == TokenType.PASSWORD_RESET)
				.filter(t -> t.isUsable(clock.instant()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired token"));
		token.getUser().changePassword(passwordEncoder.encode(newPassword));
		token.markUsed(clock.instant());
		// Whoever knew the old password is signed out everywhere (see ActiveAccountFilter).
		token.getUser().invalidateSessionsBefore(clock.instant());
	}

	/**
	 * Admin provisioning: creates a TEACHER whose password nobody knows (a random secret, hashed
	 * and never returned) and emails them a single-use invitation link; the teacher picks their own
	 * password in {@link #acceptTeacherInvite}. The admin never sees or sets it.
	 */
	@Transactional
	public User inviteTeacher(String email, String displayName) {
		String normalizedEmail = EmailAddress.normalize(email);
		if (userRepository.existsAnyByEmail(normalizedEmail)) {
			throw new EmailTakenException();
		}
		User teacher = new User(normalizedEmail, passwordEncoder.encode(tokenGenerator.generateRawToken()), Role.TEACHER,
				displayName.strip(), "uk");
		try {
			// Flush now so a concurrent insert of the same email trips the unique constraint here.
			teacher = userRepository.saveAndFlush(teacher);
		} catch (DataIntegrityViolationException e) {
			throw new EmailTakenException();
		}
		issueTeacherInviteToken(teacher);
		return teacher;
	}

	/** Re-sends a not-yet-accepted invitation; any earlier unused invitation links stop working. */
	@Transactional
	public User resendTeacherInvite(UUID teacherId) {
		User teacher = userRepository.findById(teacherId)
				.filter(u -> u.getRole() == Role.TEACHER && !u.isDeleted())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));
		if (teacher.isEmailVerified()) {
			throw new InviteAlreadyAcceptedException();
		}
		invalidateUnusedTokens(teacher, TokenType.TEACHER_INVITE);
		issueTeacherInviteToken(teacher);
		return teacher;
	}

	/**
	 * Sets the invited teacher's password. Every failure — unknown, wrong type, expired, used, or
	 * the teacher deactivated meanwhile — is the same 400, like reset-password, so the endpoint
	 * reveals nothing about why. Following the emailed link proves the mailbox, so the email
	 * counts as verified.
	 */
	@Transactional
	public void acceptTeacherInvite(String rawToken, String password) {
		Instant now = clock.instant();
		UserToken token = userTokenRepository.findByTokenHash(tokenGenerator.hash(rawToken))
				.filter(t -> t.getType() == TokenType.TEACHER_INVITE)
				.filter(t -> t.isUsable(now))
				.filter(t -> !t.getUser().isDeleted())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired token"));
		User teacher = token.getUser();
		teacher.changePassword(passwordEncoder.encode(password));
		teacher.markEmailVerified(now);
		token.markUsed(now);
		invalidateUnusedTokens(teacher, TokenType.TEACHER_INVITE);
	}

	/** Same normalization for every newly provisioned email: surrounding whitespace off, lowercase. */

	private void issueTeacherInviteToken(User teacher) {
		String rawToken = tokenGenerator.generateRawToken();
		Instant expiresAt = clock.instant().plus(authProperties.teacherInviteTtl());
		userTokenRepository.save(new UserToken(teacher, TokenType.TEACHER_INVITE, tokenGenerator.hash(rawToken), expiresAt));
		eventPublisher.publishEvent(new TeacherInviteRequestedEvent(teacher, rawToken));
	}

	private void invalidateUnusedTokens(User user, TokenType type) {
		Instant now = clock.instant();
		userTokenRepository.findByUserAndTypeAndUsedAtIsNull(user, type).forEach(t -> t.markUsed(now));
	}

	private void issueEmailVerificationToken(User user) {
		String rawToken = tokenGenerator.generateRawToken();
		Instant expiresAt = clock.instant().plus(authProperties.emailVerificationTtl());
		userTokenRepository.save(new UserToken(user, TokenType.EMAIL_VERIFICATION, tokenGenerator.hash(rawToken), expiresAt));
		eventPublisher.publishEvent(new EmailVerificationRequestedEvent(user, rawToken));
	}

	private void issuePasswordResetToken(User user) {
		String rawToken = tokenGenerator.generateRawToken();
		Instant expiresAt = clock.instant().plus(authProperties.passwordResetTtl());
		userTokenRepository.save(new UserToken(user, TokenType.PASSWORD_RESET, tokenGenerator.hash(rawToken), expiresAt));
		eventPublisher.publishEvent(new PasswordResetRequestedEvent(user, rawToken));
	}

	private boolean recentTokenIssued(User user, TokenType type) {
		return userTokenRepository.findFirstByUserAndTypeOrderByCreatedAtDesc(user, type)
				.map(t -> t.getCreatedAt().isAfter(clock.instant().minusSeconds(60)))
				.orElse(false);
	}

}
