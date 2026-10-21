package ua.kidlearn.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import ua.kidlearn.support.ClockedWebIntegrationTest;
import ua.kidlearn.users.Role;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

/**
 * POST /api/auth/reauthenticate — the adult's password when leaving child mode. Its failures count
 * against the same per-account / per-IP limits as login (5 per account, 20 per IP, 15 minutes), and
 * the session is never touched. Each test uses its own account and IP, like RateLimitingTest, since
 * the in-memory RateLimiter is shared.
 */
@Transactional
class ReauthenticationTest extends ClockedWebIntegrationTest {

	private static final String PASSWORD = "supersecret1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private static String uniqueIp() {
		return "ip-" + UUID.randomUUID();
	}

	private String newParent() {
		String email = "reauth-" + UUID.randomUUID() + "@example.test";
		User user = new User(email, passwordEncoder.encode(PASSWORD), Role.PARENT, "Test", "uk");
		user.markEmailVerified(Instant.now());
		userRepository.save(user);
		return email;
	}

	private MockHttpSession login(String email, String ip) throws Exception {
		MvcResult result = mockMvc.perform(loginRequest(email, ip))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private static MockHttpServletRequestBuilder loginRequest(String email, String ip) {
		return post("/login").with(csrf())
				.header("X-Forwarded-For", ip)
				.param("username", email)
				.param("password", PASSWORD);
	}

	private ResultActions reauthenticate(MockHttpSession session, String ip, String password) throws Exception {
		return mockMvc.perform(post("/api/auth/reauthenticate").with(csrf())
				.session(session)
				.header("X-Forwarded-For", ip)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"password\":\"%s\"}".formatted(password)));
	}

	private void stillSignedIn(MockHttpSession session) throws Exception {
		mockMvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk());
	}

	@Test
	void rightPasswordIs204AndKeepsTheSession() throws Exception {
		String ip = uniqueIp();
		MockHttpSession session = login(newParent(), ip);
		reauthenticate(session, ip, PASSWORD).andExpect(status().isNoContent());
		stillSignedIn(session);
	}

	@Test
	void wrongPasswordIs401WithoutDetailAndKeepsTheSession() throws Exception {
		String ip = uniqueIp();
		MockHttpSession session = login(newParent(), ip);
		reauthenticate(session, ip, "wrong-password")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"));
		stillSignedIn(session);
	}

	@Test
	void blankPasswordIs400AndAnonymousIs401() throws Exception {
		String ip = uniqueIp();
		MockHttpSession session = login(newParent(), ip);
		reauthenticate(session, ip, "").andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/auth/reauthenticate").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"password\":\"%s\"}".formatted(PASSWORD)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void fiveFailuresLockTheAccountEvenForTheRightPasswordUntilTheWindowPasses() throws Exception {
		String ip = uniqueIp();
		MockHttpSession session = login(newParent(), ip);
		for (int i = 0; i < 5; i++) {
			reauthenticate(session, ip, "wrong-password").andExpect(status().isUnauthorized());
		}
		reauthenticate(session, ip, PASSWORD)
				.andExpect(status().isTooManyRequests())
				.andExpect(header().exists("Retry-After"))
				.andExpect(jsonPath("$.code").value("RATE_LIMITED"));
		stillSignedIn(session);

		testClock.advance(Duration.ofMinutes(16));
		reauthenticate(session, ip, PASSWORD).andExpect(status().isNoContent());
	}

	@Test
	void reauthenticationFailuresCountTowardsTheLoginLimitOfTheSameAccount() throws Exception {
		String email = newParent();
		String ip = uniqueIp();
		MockHttpSession session = login(email, ip);
		for (int i = 0; i < 5; i++) {
			reauthenticate(session, ip, "wrong-password").andExpect(status().isUnauthorized());
		}
		// A fresh login with the right password is throttled too: no way around the login limit.
		mockMvc.perform(loginRequest(email, uniqueIp()))
				.andExpect(status().isTooManyRequests());
	}

	@Test
	void failuresFromOneIpAcrossAccountsHitThePerIpLimit() throws Exception {
		String ip = uniqueIp();
		for (int account = 0; account < 5; account++) {
			MockHttpSession session = login(newParent(), ip);
			for (int i = 0; i < 4; i++) {
				reauthenticate(session, ip, "wrong-password").andExpect(status().isUnauthorized());
			}
		}
		// 20 failures from this IP: even a different account's right password is refused from it…
		MockHttpSession other = login(newParent(), uniqueIp());
		reauthenticate(other, ip, PASSWORD).andExpect(status().isTooManyRequests());
		// …but not from another IP.
		reauthenticate(other, uniqueIp(), PASSWORD).andExpect(status().isNoContent());
	}

	@Test
	void successResetsTheAccountCounter() throws Exception {
		String ip = uniqueIp();
		MockHttpSession session = login(newParent(), ip);
		for (int i = 0; i < 4; i++) {
			reauthenticate(session, ip, "wrong-password").andExpect(status().isUnauthorized());
		}
		reauthenticate(session, ip, PASSWORD).andExpect(status().isNoContent());
		reauthenticate(session, ip, "wrong-password").andExpect(status().isUnauthorized());
		reauthenticate(session, ip, PASSWORD).andExpect(status().isNoContent());
	}

}
