package ua.kidlearn.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ua.kidlearn.ratelimit.RateLimitProperties;
import ua.kidlearn.users.UserRepository;

/**
 * QA1-022: a parents' meeting behind one address. With the production defaults (the prod-like
 * "guardtest" profile — no dev overrides), 25 parents sign up, confirm their email and log in from
 * the same IP without a single 429, while one account is still locked after 5 wrong passwords.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("guardtest")
@Transactional
class SharedAddressRateLimitTest {

	private static final String PASSWORD = "supersecret1";
	private static final String SHARED_IP = "10.77.0.1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserTokenRepository userTokenRepository;

	@Autowired
	private TokenGenerator tokenGenerator;

	@Autowired
	private RateLimitProperties properties;

	@Test
	void productionDefaultsAreSizedForASharedAddressAndKeepThePerAccountLimits() {
		assertThat(properties.registerPerIp()).isEqualTo(new RateLimitProperties.Limit(60, Duration.ofHours(1)));
		assertThat(properties.verifyEmailPerIp()).isEqualTo(new RateLimitProperties.Limit(60, Duration.ofMinutes(15)));
		assertThat(properties.resendVerificationPerIp()).isEqualTo(new RateLimitProperties.Limit(30, Duration.ofMinutes(15)));
		assertThat(properties.forgotPasswordPerIp()).isEqualTo(new RateLimitProperties.Limit(30, Duration.ofMinutes(15)));
		assertThat(properties.acceptInvitePerIp()).isEqualTo(new RateLimitProperties.Limit(60, Duration.ofMinutes(15)));
		assertThat(properties.loginPerIp()).isEqualTo(new RateLimitProperties.Limit(100, Duration.ofMinutes(15)));
		assertThat(properties.loginPerAccount()).isEqualTo(new RateLimitProperties.Limit(5, Duration.ofMinutes(15)));
		assertThat(properties.twoFactorPerAccount()).isEqualTo(new RateLimitProperties.Limit(5, Duration.ofMinutes(15)));
		assertThat(properties.twoFactorPerIp()).isEqualTo(new RateLimitProperties.Limit(5, Duration.ofMinutes(15)));
	}

	@Test
	void twentyFiveParentsBehindOneAddressSignUpConfirmAndLogInButOneAccountStillLocks() throws Exception {
		String run = UUID.randomUUID().toString().substring(0, 8);
		for (int i = 0; i < 25; i++) {
			String email = "meeting-" + run + "-" + i + "@example.test";
			int registered = mockMvc.perform(post("/api/auth/register").with(csrf()).header("X-Forwarded-For", SHARED_IP)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"email\":\"%s\",\"password\":\"%s\",\"displayName\":\"Батьки\"}".formatted(email, PASSWORD)))
					.andReturn().getResponse().getStatus();
			assertThat(registered).as("register #%d", i + 1).isBetween(200, 299);

			String rawToken = tokenGenerator.generateRawToken();
			userTokenRepository.save(new UserToken(userRepository.findActiveByEmail(email).orElseThrow(),
					TokenType.EMAIL_VERIFICATION, tokenGenerator.hash(rawToken), Instant.now().plus(Duration.ofHours(1))));
			int verified = mockMvc.perform(post("/api/auth/verify-email").with(csrf()).header("X-Forwarded-For", SHARED_IP)
					.contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"%s\"}".formatted(rawToken)))
					.andReturn().getResponse().getStatus();
			assertThat(verified).as("verify #%d", i + 1).isEqualTo(200);

			int login = login(email, PASSWORD);
			assertThat(login).as("login #%d", i + 1).isEqualTo(302);
		}

		String locked = "meeting-" + run + "-0@example.test";
		for (int attempt = 1; attempt <= 5; attempt++) {
			assertThat(login(locked, "wrong-password-" + attempt)).as("wrong password #%d", attempt).isNotEqualTo(429);
		}
		assertThat(login(locked, "wrong-password-6")).isEqualTo(429);
		// The correct password doesn't get through the lock either.
		assertThat(login(locked, PASSWORD)).isEqualTo(429);
	}

	private int login(String email, String password) throws Exception {
		return mockMvc.perform(post("/login").with(csrf()).header("X-Forwarded-For", SHARED_IP)
				.param("username", email).param("password", password))
				.andReturn().getResponse().getStatus();
	}

}
