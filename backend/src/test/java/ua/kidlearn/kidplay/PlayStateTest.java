package ua.kidlearn.kidplay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import ua.kidlearn.ratelimit.MutableClockTestConfig;
import ua.kidlearn.ratelimit.MutableTestClock;
import ua.kidlearn.users.Role;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

/**
 * Server-enforced play session (docs/UX_гайд_дитячого_режиму §6): the limit only applies when a
 * lesson completes, a long pause starts a new session, resting refuses lesson starts until it ends
 * or a parent wakes the child. Time is driven by the test Clock (defaults: rest 60m, gap 30m).
 * Fixture: a verified parent whose demo child has the two dev-seed lessons assigned.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import(MutableClockTestConfig.class)
@Transactional
class PlayStateTest {

	private static final String PASSWORD = "supersecret1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MutableTestClock testClock;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManager entityManager;

	private MockHttpSession parent;
	private UUID childId;
	private UUID lessonVersionId;

	private static String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@example.test";
	}

	private MockHttpSession login(String email) throws Exception {
		MvcResult result = mockMvc.perform(formLogin().user(email).password(PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private MockHttpSession verifiedUser(Role role) throws Exception {
		String email = uniqueEmail(role.name().toLowerCase());
		User user = new User(email, passwordEncoder.encode(PASSWORD), role, "Test", "uk");
		user.markEmailVerified();
		userRepository.save(user);
		return login(email);
	}

	@BeforeEach
	void seedChildWithLessons() throws Exception {
		parent = verifiedUser(Role.PARENT);
		MvcResult seed = mockMvc.perform(post("/api/dev/seed-demo").with(csrf()).session(parent))
				.andExpect(status().isOk())
				.andReturn();
		String body = seed.getResponse().getContentAsString();
		childId = UUID.fromString(JsonPath.read(body, "$.childId"));
		lessonVersionId = UUID.fromString(JsonPath.read(body, "$.lessonVersionId"));
	}

	private ResultActions startLesson() throws Exception {
		return mockMvc.perform(post("/api/children/" + childId + "/attempts").with(csrf())
				.session(parent)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"lessonVersionId\":\"%s\"}".formatted(lessonVersionId)));
	}

	private String startedAttempt() throws Exception {
		MvcResult result = startLesson().andExpect(status().isCreated()).andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.attemptId");
	}

	private ResultActions complete(String attemptId) throws Exception {
		return mockMvc.perform(post("/api/attempts/" + attemptId + "/complete").with(csrf())
				.session(parent)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"result\":\"completed\",\"score\":1}"));
	}

	private ResultActions setLimit(MockHttpSession session, String limitJson) throws Exception {
		return mockMvc.perform(put("/api/children/" + childId + "/play-settings").with(csrf())
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"sessionLimitMinutes\":" + limitJson + "}"));
	}

	private ResultActions playState(MockHttpSession session) throws Exception {
		return mockMvc.perform(get("/api/children/" + childId + "/play-state").session(session));
	}

	private void restAfterOneLongLesson() throws Exception {
		setLimit(parent, "10").andExpect(status().isOk());
		String attemptId = startedAttempt();
		testClock.advance(Duration.ofMinutes(11));
		complete(attemptId).andExpect(status().isOk()).andExpect(jsonPath("$.resting").value(true));
	}

	@Test
	void freshChildIsNotRestingWithTheDefaultFifteenMinuteLimit() throws Exception {
		playState(parent)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resting").value(false))
				.andExpect(jsonPath("$.restUntil").doesNotExist())
				.andExpect(jsonPath("$.sessionLimitMinutes").value(15));
	}

	@Test
	void limitNeverInterruptsALessonAndAppliesOnlyWhenItCompletes() throws Exception {
		setLimit(parent, "10").andExpect(status().isOk());
		String attemptId = startedAttempt();

		testClock.advance(Duration.ofMinutes(25));
		// Well past the limit, mid-lesson: answers are still recorded and the child isn't resting yet.
		mockMvc.perform(post("/api/attempts/" + attemptId + "/answers").with(csrf())
						.session(parent)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sceneKey\":\"s2\",\"tryNo\":1,\"chosenOption\":\"a\",\"isCorrect\":true,\"hintsUsed\":0}"))
				.andExpect(status().isCreated());
		playState(parent).andExpect(jsonPath("$.resting").value(false));

		Instant completedAt = testClock.instant();
		MvcResult completed = complete(attemptId)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resting").value(true))
				.andReturn();
		assertThat(restUntil(completed)).isEqualTo(micros(completedAt.plus(Duration.ofMinutes(60))));
		playState(parent).andExpect(jsonPath("$.resting").value(true));
	}

	@Test
	void lessonEndingBeforeTheLimitKeepsPlaying() throws Exception {
		setLimit(parent, "10").andExpect(status().isOk());
		String attemptId = startedAttempt();
		testClock.advance(Duration.ofMinutes(9));
		complete(attemptId).andExpect(status().isOk()).andExpect(jsonPath("$.resting").value(false));
		startLesson().andExpect(status().isCreated());
	}

	@Test
	void startWhileRestingIsRefusedWithRestUntilUntilTheRestIsOver() throws Exception {
		restAfterOneLongLesson();
		Instant restUntil = testClock.instant().plus(Duration.ofMinutes(60));

		MvcResult refused = startLesson()
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("CHILD_RESTING"))
				.andReturn();
		assertThat(restUntil(refused)).isEqualTo(micros(restUntil));

		testClock.advance(Duration.ofMinutes(59));
		startLesson().andExpect(status().isConflict());

		testClock.advance(Duration.ofMinutes(2));
		playState(parent).andExpect(jsonPath("$.resting").value(false));
		startLesson().andExpect(status().isCreated());
	}

	@Test
	void restingIsKeptAcrossNewRequestsSoAReloadDoesNotGetAroundIt() throws Exception {
		restAfterOneLongLesson();
		// A "reload" is just a new request with the same session — and a fresh session sees it too.
		playState(parent).andExpect(jsonPath("$.resting").value(true));
		startLesson().andExpect(status().isConflict());
	}

	@Test
	void pauseLongerThanTheGapStartsANewSession() throws Exception {
		setLimit(parent, "10").andExpect(status().isOk());
		complete(startedAttemptThenAdvance(Duration.ofMinutes(5))).andExpect(jsonPath("$.resting").value(false));

		testClock.advance(Duration.ofMinutes(31));
		// New session from here: 6 more minutes is under the limit.
		complete(startedAttemptThenAdvance(Duration.ofMinutes(6))).andExpect(jsonPath("$.resting").value(false));
	}

	@Test
	void pauseShorterThanTheGapContinuesTheSession() throws Exception {
		setLimit(parent, "10").andExpect(status().isOk());
		complete(startedAttemptThenAdvance(Duration.ofMinutes(5))).andExpect(jsonPath("$.resting").value(false));

		testClock.advance(Duration.ofMinutes(20));
		// Same session, started 25 minutes ago: over the limit once this lesson ends.
		complete(startedAttemptThenAdvance(Duration.ofMinutes(1))).andExpect(jsonPath("$.resting").value(true));
	}

	private String startedAttemptThenAdvance(Duration lessonLength) throws Exception {
		String attemptId = startedAttempt();
		testClock.advance(lessonLength);
		return attemptId;
	}

	@Test
	void parentWakeEndsTheRestRightAway() throws Exception {
		restAfterOneLongLesson();

		mockMvc.perform(post("/api/children/" + childId + "/play-state/wake").with(csrf()).session(parent))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resting").value(false));
		playState(parent).andExpect(jsonPath("$.resting").value(false));
		startLesson().andExpect(status().isCreated());
	}

	@Test
	void noLimitNeverRests() throws Exception {
		setLimit(parent, "null").andExpect(status().isOk()).andExpect(jsonPath("$.sessionLimitMinutes").doesNotExist());
		complete(startedAttemptThenAdvance(Duration.ofHours(3))).andExpect(jsonPath("$.resting").value(false));
	}

	@Test
	void onlyTheOfferedLimitsAreAccepted() throws Exception {
		for (String allowed : new String[] {"10", "15", "20", "30"}) {
			setLimit(parent, allowed).andExpect(status().isOk())
					.andExpect(jsonPath("$.sessionLimitMinutes").value(Integer.parseInt(allowed)));
		}
		for (String rejected : new String[] {"0", "5", "25", "60", "-10"}) {
			setLimit(parent, rejected).andExpect(status().isBadRequest());
		}
		playState(parent).andExpect(jsonPath("$.sessionLimitMinutes").value(30));
	}

	@Test
	void anotherParentGets404AndATeacherIsForbidden() throws Exception {
		MockHttpSession stranger = verifiedUser(Role.PARENT);
		playState(stranger).andExpect(status().isNotFound());
		setLimit(stranger, "null").andExpect(status().isNotFound());
		mockMvc.perform(post("/api/children/" + childId + "/play-state/wake").with(csrf()).session(stranger))
				.andExpect(status().isNotFound());

		MockHttpSession teacher = verifiedUser(Role.TEACHER);
		playState(teacher).andExpect(status().isForbidden());
		setLimit(teacher, "null").andExpect(status().isForbidden());
		mockMvc.perform(post("/api/children/" + childId + "/play-state/wake").with(csrf()).session(teacher))
				.andExpect(status().isForbidden());

		// Nothing the stranger or the teacher sent changed the child's settings.
		playState(parent).andExpect(jsonPath("$.sessionLimitMinutes").value(15));
	}

	@Test
	void deletingTheChildDeletesItsPlayState() throws Exception {
		restAfterOneLongLesson();
		assertThat(playStateRows()).isEqualTo(1);

		mockMvc.perform(delete("/api/children/" + childId).with(csrf()).session(parent))
				.andExpect(status().isNoContent());
		entityManager.flush();

		assertThat(playStateRows()).isZero();
	}

	private static Instant restUntil(MvcResult result) throws Exception {
		return micros(Instant.parse(JsonPath.read(result.getResponse().getContentAsString(), "$.restUntil")));
	}

	// timestamptz keeps microseconds; the test clock has nanoseconds.
	private static Instant micros(Instant instant) {
		return instant.truncatedTo(ChronoUnit.MICROS);
	}

	private Integer playStateRows() {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM child_play_state WHERE child_id = ?", Integer.class,
				childId);
	}

}
