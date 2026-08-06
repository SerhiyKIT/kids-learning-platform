package ua.kidlearn.ratelimit;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rate limiting on auth endpoints: failure-counting login throttle (per account + per IP) and
 * per-IP request counting on the other sensitive auth POSTs. Deterministic via the test Clock —
 * no sleeps; time is advanced explicitly. Each test uses its own unique email/IP so the
 * in-memory RateLimiter (a shared singleton across all tests in this class) never leaks state
 * between them.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import(MutableClockTestConfig.class)
@Transactional
class RateLimitingTest {

	private static final String PASSWORD = "supersecret1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MutableTestClock testClock;

	private static String uniqueEmail() {
		return "ratelimit-" + UUID.randomUUID() + "@example.test";
	}

	private static String uniqueIp() {
		return "ip-" + UUID.randomUUID();
	}

	private void register(String email, String ip) throws Exception {
		mockMvc.perform(post("/api/auth/register").with(csrf())
						.header("X-Forwarded-For", ip)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"%s\",\"password\":\"%s\",\"displayName\":\"Test\"}".formatted(email, PASSWORD)))
				.andExpect(status().isCreated());
	}

	private ResultActions failLogin(String email, String ip) throws Exception {
		return mockMvc.perform(post("/login").with(csrf())
				.header("X-Forwarded-For", ip)
				.param("username", email)
				.param("password", "wrong-password"));
	}

	private ResultActions succeedLogin(String email, String ip) throws Exception {
		return mockMvc.perform(post("/login").with(csrf())
				.header("X-Forwarded-For", ip)
				.param("username", email)
				.param("password", PASSWORD));
	}

	@Test
	void accountLockedAfterMaxFailuresButDifferentAccountUnaffected() throws Exception {
		String email = uniqueEmail();
		String ip = uniqueIp();
		register(email, ip);

		for (int i = 0; i < 5; i++) {
			failLogin(email, ip).andExpect(status().is3xxRedirection());
		}
		failLogin(email, ip)
				.andExpect(status().isTooManyRequests())
				.andExpect(header().exists("Retry-After"));

		String otherEmail = uniqueEmail();
		String otherIp = uniqueIp();
		register(otherEmail, otherIp);
		succeedLogin(otherEmail, otherIp).andExpect(status().is3xxRedirection());
	}

	@Test
	void loginAllowedAgainAfterWindowElapses() throws Exception {
		String email = uniqueEmail();
		String ip = uniqueIp();
		register(email, ip);

		for (int i = 0; i < 5; i++) {
			failLogin(email, ip).andExpect(status().is3xxRedirection());
		}
		failLogin(email, ip).andExpect(status().isTooManyRequests());

		testClock.advance(Duration.ofMinutes(16));

		failLogin(email, ip).andExpect(status().is3xxRedirection());
	}

	@Test
	void successfulLoginResetsAccountFailureCounter() throws Exception {
		String email = uniqueEmail();
		String ip = uniqueIp();
		register(email, ip);

		for (int i = 0; i < 4; i++) {
			failLogin(email, ip).andExpect(status().is3xxRedirection());
		}
		succeedLogin(email, ip).andExpect(status().is3xxRedirection());

		// Counter was reset by the success, so this single new failure doesn't lock the account.
		failLogin(email, ip).andExpect(status().is3xxRedirection());
	}

	@Test
	void forgotPasswordOverIpLimitReturns429ButOtherIpUnaffected() throws Exception {
		String ip = uniqueIp();
		for (int i = 0; i < 5; i++) {
			forgotPassword(uniqueEmail(), ip).andExpect(status().isOk());
		}
		forgotPassword(uniqueEmail(), ip)
				.andExpect(status().isTooManyRequests())
				.andExpect(header().exists("Retry-After"));

		String otherIp = uniqueIp();
		forgotPassword(uniqueEmail(), otherIp).andExpect(status().isOk());
	}

	@Test
	void normalRegisterLoginAndForgotPasswordUnderLimitBehaveAsBefore() throws Exception {
		String email = uniqueEmail();
		String ip = uniqueIp();
		register(email, ip);
		succeedLogin(email, ip).andExpect(status().is3xxRedirection());
		forgotPassword(email, ip).andExpect(status().isOk());
	}

	private ResultActions forgotPassword(String email, String ip) throws Exception {
		return mockMvc.perform(post("/api/auth/forgot-password").with(csrf())
				.header("X-Forwarded-For", ip)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\"}".formatted(email)));
	}

}
