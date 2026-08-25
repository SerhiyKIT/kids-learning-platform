package ua.kidlearn.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import ua.kidlearn.users.Role;
import ua.kidlearn.users.UserRepository;

/**
 * Covers BootstrapController with app.bootstrap.admin-token configured (see
 * BootstrapControllerTokenNotConfiguredTest for the blank-token/disabled case). Each test uses
 * its own unique client IP so the shared in-memory RateLimiter singleton doesn't leak counts
 * between tests; @Transactional rolls back any created users after each test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = "app.bootstrap.admin-token=correct-horse-battery-staple")
@Transactional
class BootstrapControllerTest {

	private static final String TOKEN = "correct-horse-battery-staple";
	private static final String PASSWORD = "supersecret1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	private static String uniqueEmail() {
		return "bootstrap-" + UUID.randomUUID() + "@example.test";
	}

	private static String uniqueIp() {
		return "ip-" + UUID.randomUUID();
	}

	private ResultActions bootstrap(String token, String email, String password, String ip) throws Exception {
		return mockMvc.perform(post("/api/bootstrap/admin").with(csrf())
				.header("X-Forwarded-For", ip)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"token\":\"%s\",\"email\":\"%s\",\"password\":\"%s\",\"displayName\":\"First Admin\"}"
						.formatted(token, email, password)));
	}

	@Test
	void correctTokenCreatesFirstAdmin() throws Exception {
		String email = uniqueEmail();

		bootstrap(TOKEN, email, PASSWORD, uniqueIp())
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.role").value("ADMIN"));

		assertThat(userRepository.existsByRoleAndDeletedAtIsNull(Role.ADMIN)).isTrue();
	}

	@Test
	void wrongTokenReturnsUnauthorizedAndCreatesNoAdmin() throws Exception {
		bootstrap("not-the-token", uniqueEmail(), PASSWORD, uniqueIp())
				.andExpect(status().isUnauthorized());

		assertThat(userRepository.existsByRoleAndDeletedAtIsNull(Role.ADMIN)).isFalse();
	}

	@Test
	void alreadyBootstrappedReturnsGoneEvenWithCorrectToken() throws Exception {
		String ip = uniqueIp();
		bootstrap(TOKEN, uniqueEmail(), PASSWORD, ip).andExpect(status().isCreated());

		bootstrap(TOKEN, uniqueEmail(), PASSWORD, ip)
				.andExpect(status().isGone());
	}

	@Test
	void passwordTooShortReturnsUnprocessableEntity() throws Exception {
		bootstrap(TOKEN, uniqueEmail(), "short1", uniqueIp())
				.andExpect(status().isUnprocessableEntity());

		assertThat(userRepository.existsByRoleAndDeletedAtIsNull(Role.ADMIN)).isFalse();
	}

	@Test
	void repeatedWrongTokenAttemptsFromOneIpEventuallyRateLimited() throws Exception {
		String ip = uniqueIp();

		for (int i = 0; i < 5; i++) {
			bootstrap("wrong-token", uniqueEmail(), PASSWORD, ip).andExpect(status().isUnauthorized());
		}

		bootstrap("wrong-token", uniqueEmail(), PASSWORD, ip)
				.andExpect(status().isTooManyRequests())
				.andExpect(header().exists("Retry-After"));
	}

}
