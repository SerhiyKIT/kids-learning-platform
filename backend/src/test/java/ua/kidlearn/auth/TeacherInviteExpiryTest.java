package ua.kidlearn.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import ua.kidlearn.support.ClockedWebIntegrationTest;

/**
 * Teacher invitation expiry, driven by the injected test Clock (no sleeping). The raw token is
 * read from the recorded TeacherInviteRequestedEvent — exactly what the email would carry —
 * instead of mocking the mail sender, so this shares the cached Spring context of RateLimitingTest
 * and TwoFactorFlowTest (same configuration) rather than opening yet another one with its own DB
 * pool. Invites through AuthService directly: no admin login, so no system-clock TOTP that a
 * shifted test Clock would break. The shared Clock is shifted back in @AfterEach.
 */
@RecordApplicationEvents
@Transactional
class TeacherInviteExpiryTest extends ClockedWebIntegrationTest {

	private static final String TEACHER_PASSWORD = "teacher-chosen-pass-1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuthService authService;

	@Autowired
	private ApplicationEvents events;

	private Duration clockShift = Duration.ZERO;

	@AfterEach
	void restoreClock() {
		testClock.advance(clockShift.negated());
		clockShift = Duration.ZERO;
	}

	private void advance(Duration duration) {
		testClock.advance(duration);
		clockShift = clockShift.plus(duration);
	}

	private String inviteAndCaptureToken() {
		UUID teacherId = authService.inviteTeacher("teacher-" + UUID.randomUUID() + "@example.test", "Вчитель").getId();
		return events.stream(TeacherInviteRequestedEvent.class)
				.filter(e -> e.user().getId().equals(teacherId))
				.findFirst()
				.orElseThrow()
				.rawToken();
	}

	private ResultActions accept(String token) throws Exception {
		return mockMvc.perform(post("/api/auth/accept-invite").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"token\":\"%s\",\"password\":\"%s\"}".formatted(token, TEACHER_PASSWORD)));
	}

	@Test
	void inviteIsRejectedOnceItsTtlHasPassed() throws Exception {
		String token = inviteAndCaptureToken();

		advance(Duration.ofHours(72).plusMinutes(1)); // default teacher-invite-ttl is 72h

		accept(token).andExpect(status().isBadRequest());
	}

	@Test
	void inviteStillWorksJustBeforeItsTtl() throws Exception {
		String token = inviteAndCaptureToken();

		advance(Duration.ofHours(71).plusMinutes(59));

		accept(token).andExpect(status().isOk());
	}

}
