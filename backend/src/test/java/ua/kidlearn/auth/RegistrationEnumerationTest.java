package ua.kidlearn.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import ua.kidlearn.support.WebIntegrationTest;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

/**
 * QA1-006: POST /api/auth/register answers exactly the same whether the address is new, taken by
 * a verified account or taken by an unverified one; only the mailbox learns which. No
 * @Transactional: the emails are sent after commit. Users are removed in @AfterEach.
 */
class RegistrationEnumerationTest extends WebIntegrationTest {

	private static final String PASSWORD = "supersecret1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JdbcTemplate jdbc;

	private final List<String> emails = new ArrayList<>();

	@BeforeEach
	void resetMailMock() {
		Mockito.reset(authMailSender);
	}

	@AfterEach
	void cleanUp() {
		emails.forEach(email -> userRepository.findActiveByEmail(email).ifPresent(userRepository::delete));
	}

	private String email() {
		String email = "enum-" + UUID.randomUUID() + "@example.test";
		emails.add(email);
		return email;
	}

	private MockHttpServletResponse register(String email, String password, String name) throws Exception {
		return mockMvc.perform(post("/api/auth/register").with(csrf())
						.header("X-Forwarded-For", "ip-" + UUID.randomUUID())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"%s\",\"password\":\"%s\",\"displayName\":\"%s\"}".formatted(email, password, name)))
				.andReturn().getResponse();
	}

	private static ArgumentMatcher<User> owner(String email) {
		return user -> user.getEmail().equals(email);
	}

	@Test
	void newVerifiedAndUnverifiedAddressesGetTheSameAnswer() throws Exception {
		String fresh = email();
		MockHttpServletResponse first = register(fresh, PASSWORD, "Мама");
		verify(authMailSender, timeout(5_000)).sendVerification(Mockito.argThat(owner(fresh)), any());

		String verified = email();
		register(verified, PASSWORD, "Мама");
		User user = userRepository.findActiveByEmail(verified).orElseThrow();
		user.markEmailVerified(Instant.now());
		userRepository.saveAndFlush(user);
		String hashBefore = userRepository.findActiveByEmail(verified).orElseThrow().getPasswordHash();
		MockHttpServletResponse takenVerified = register(verified.toUpperCase(), "another-password-2", "Хтось інший");

		MockHttpServletResponse takenUnverified = register(fresh, "another-password-2", "Хтось інший");

		for (MockHttpServletResponse response : List.of(first, takenVerified, takenUnverified)) {
			assertThat(response.getStatus()).isEqualTo(202);
			assertThat(response.getContentAsString()).isEqualTo(first.getContentAsString());
		}
		assertThat(first.getContentAsString()).doesNotContain(fresh).doesNotContain("id");

		// The verified account isn't touched; its owner hears about the attempt.
		User untouched = userRepository.findActiveByEmail(verified).orElseThrow();
		assertThat(untouched.getPasswordHash()).isEqualTo(hashBefore);
		assertThat(untouched.getDisplayName()).isEqualTo("Мама");
		verify(authMailSender, timeout(5_000)).sendAlreadyRegistered(Mockito.argThat(owner(verified)));
		verify(authMailSender, after(500).times(1)).sendVerification(Mockito.argThat(owner(verified)), any());
		assertThat(userRepository.findAll()).filteredOn(u -> u.getEmail().equals(fresh)).hasSize(1);
	}

	@Test
	void anUnverifiedAddressGetsTheVerificationAgainUnderTheAntiSpamWindow() throws Exception {
		String unverified = email();
		register(unverified, PASSWORD, "Мама");
		verify(authMailSender, timeout(5_000).times(1)).sendVerification(Mockito.argThat(owner(unverified)), any());

		// Within a minute: no second email (same anti-spam as "resend").
		register(unverified, PASSWORD, "Мама");
		verify(authMailSender, after(500).times(1)).sendVerification(Mockito.argThat(owner(unverified)), any());

		jdbc.update("UPDATE user_tokens SET created_at = now() - interval '2 minutes' WHERE user_id = ?",
				userRepository.findActiveByEmail(unverified).orElseThrow().getId());
		register(unverified, PASSWORD, "Мама");
		verify(authMailSender, timeout(5_000).times(2)).sendVerification(Mockito.argThat(owner(unverified)), any());
		verify(authMailSender, never()).sendAlreadyRegistered(any());
	}

	@Test
	void theAlreadyRegisteredEmailIsNotRepeatedWithinFifteenMinutes() throws Exception {
		String verified = email();
		register(verified, PASSWORD, "Мама");
		User user = userRepository.findActiveByEmail(verified).orElseThrow();
		user.markEmailVerified(Instant.now());
		userRepository.saveAndFlush(user);

		register(verified, PASSWORD, "Мама");
		register(verified, PASSWORD, "Мама");
		verify(authMailSender, after(1_000).times(1)).sendAlreadyRegistered(Mockito.argThat(owner(verified)));
	}

}
