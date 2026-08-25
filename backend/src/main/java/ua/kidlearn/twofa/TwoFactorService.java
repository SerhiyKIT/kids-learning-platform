package ua.kidlearn.twofa;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.TimeProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

/**
 * TOTP setup/enable/verify for mandatory admin 2FA. No disable endpoint by design (see the
 * package javadoc) — once {@link User#enableTotp()} has run, it's permanent for that account.
 */
@Service
public class TwoFactorService {

	private static final String ISSUER = "KidLearn Admin";
	private static final int BACKUP_CODE_COUNT = 10;
	private static final int BACKUP_CODE_DIGITS = 10;

	private static final SecureRandom RANDOM = new SecureRandom();

	private final UserRepository userRepository;
	private final AdminBackupCodeRepository backupCodeRepository;
	private final TwofaEncryptionService encryptionService;
	private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
	private final CodeGenerator codeGenerator = new DefaultCodeGenerator();
	private final CodeVerifier codeVerifier;

	public TwoFactorService(UserRepository userRepository, AdminBackupCodeRepository backupCodeRepository,
			TwofaEncryptionService encryptionService, Clock clock) {
		this.userRepository = userRepository;
		this.backupCodeRepository = backupCodeRepository;
		this.encryptionService = encryptionService;
		// Reuses the same injected Clock as RateLimiter, so tests can drive TOTP verification
		// and rate-limit windows off one deterministic time source (see MutableTestClock).
		TimeProvider timeProvider = () -> clock.instant().getEpochSecond();
		DefaultCodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
		// +/-1 step (30s each way) to tolerate clock drift between the admin's authenticator app
		// and us, per spec.
		verifier.setAllowedTimePeriodDiscrepancy(1);
		this.codeVerifier = verifier;
	}

	/** Idempotent: calling again before enabling replaces the pending secret. */
	@Transactional
	public TwoFactorSetupResponse setup(String email) {
		User user = getUser(email);
		requireNotEnabled(user);
		String secret = secretGenerator.generate();
		user.setPendingTotpSecret(encryptionService.encrypt(secret));
		String otpauthUri = new QrData.Builder()
				.label(user.getEmail())
				.secret(secret)
				.issuer(ISSUER)
				.digits(6)
				.period(30)
				.build()
				.getUri();
		return new TwoFactorSetupResponse(otpauthUri, secret);
	}

	/** Returns the plaintext backup codes — the only time they're ever available in the clear. */
	@Transactional
	public List<String> enable(String email, String code) {
		User user = getUser(email);
		requireNotEnabled(user);
		String encryptedSecret = user.getTotpSecretEnc();
		if (encryptedSecret == null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "TWO_FACTOR_SETUP_NOT_STARTED");
		}
		if (!codeVerifier.isValidCode(encryptionService.decrypt(encryptedSecret), code)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_CODE");
		}
		user.enableTotp();
		List<String> backupCodes = generateBackupCodes();
		backupCodeRepository
				.saveAll(backupCodes.stream().map(raw -> new AdminBackupCode(user.getId(), hash(raw))).toList());
		return backupCodes;
	}

	/** Accepts a valid TOTP code OR an unused backup code (marking it used). */
	@Transactional
	public boolean verify(String email, String code) {
		User user = getUser(email);
		String encryptedSecret = user.getTotpSecretEnc();
		if (encryptedSecret != null && codeVerifier.isValidCode(encryptionService.decrypt(encryptedSecret), code)) {
			return true;
		}
		return matchesAnyUnusedBackupCode(user.getId(), code);
	}

	private boolean matchesAnyUnusedBackupCode(UUID userId, String code) {
		byte[] submittedHash = hash(code).getBytes(StandardCharsets.UTF_8);
		for (AdminBackupCode candidate : backupCodeRepository.findByUserIdAndUsedAtIsNull(userId)) {
			if (MessageDigest.isEqual(submittedHash, candidate.getCodeHash().getBytes(StandardCharsets.UTF_8))) {
				candidate.markUsed();
				return true;
			}
		}
		return false;
	}

	private static void requireNotEnabled(User user) {
		if (user.isTotpEnabled()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "TWO_FACTOR_ALREADY_ENABLED");
		}
	}

	private User getUser(String email) {
		return userRepository.findByEmailAndDeletedAtIsNull(email).orElseThrow();
	}

	private static List<String> generateBackupCodes() {
		return IntStream.range(0, BACKUP_CODE_COUNT).mapToObj(i -> generateOneBackupCode()).toList();
	}

	private static String generateOneBackupCode() {
		StringBuilder code = new StringBuilder(BACKUP_CODE_DIGITS);
		for (int i = 0; i < BACKUP_CODE_DIGITS; i++) {
			code.append(RANDOM.nextInt(10));
		}
		return code.toString();
	}

	private static String hash(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}

}
