package ua.kidlearn.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import java.time.Year;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import ua.kidlearn.children.ParentChild;
import ua.kidlearn.children.ParentChildRepository;
import ua.kidlearn.support.ClockedWebIntegrationTest;
import ua.kidlearn.users.Role;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

/**
 * Privacy controls of docs/Ролі_та_приватність.md: data export (own child only, nothing about
 * others), real account deletion (sole vs shared children, audit kept de-identified), signing out
 * everywhere, password change, and the retention job. Same context configuration as PlayStateTest.
 * @Transactional: deletion audit entries are written inside the transaction on purpose, so they're
 * visible here; the DB's ON DELETE actions run at statement time and are checked with SQL.
 */
@Transactional
class PrivacyControlsTest extends ClockedWebIntegrationTest {

	private static final String PASSWORD = "supersecret1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ParentChildRepository parentChildRepository;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private RetentionService retentionService;

	private record Account(UUID id, String email) {
	}

	private Account user(Role role) {
		String email = "privacy-" + role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.test";
		User user = new User(email, passwordEncoder.encode(PASSWORD), role, "Test", "uk");
		user.markEmailVerified(Instant.now());
		return new Account(userRepository.saveAndFlush(user).getId(), email);
	}

	private MockHttpSession login(String email, String password) throws Exception {
		MvcResult result = mockMvc.perform(formLogin().user(email).password(password))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private MockHttpSession login(String email) throws Exception {
		return login(email, PASSWORD);
	}

	private UUID child(MockHttpSession parent, String name) throws Exception {
		String id = JsonPath.read(mockMvc.perform(post("/api/children").with(csrf())
						.session(parent)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"displayName\":\"%s\",\"birthYear\":%d,\"relation\":\"mother\"}"
								.formatted(name, Year.now().getValue() - 5)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString(), "$.id");
		mockMvc.perform(post("/api/children/" + id + "/consent").with(csrf()).session(parent)).andExpect(status().isOk());
		return UUID.fromString(id);
	}

	/** The parent's demo child (dev seed) with one completed attempt holding two answers. */
	private UUID seededChildWithHistory(MockHttpSession parent) throws Exception {
		String body = mockMvc.perform(post("/api/dev/seed-demo").with(csrf()).session(parent))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		UUID childId = UUID.fromString(JsonPath.read(body, "$.childId"));
		String versionId = JsonPath.read(body, "$.lessonVersionId");
		String attemptId = JsonPath.read(mockMvc.perform(post("/api/children/" + childId + "/attempts").with(csrf())
						.session(parent)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"lessonVersionId\":\"%s\"}".formatted(versionId)))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.attemptId");
		for (String answer : new String[] {"{\"sceneKey\":\"scene_2\",\"tryNo\":1,\"chosenOption\":\"b\",\"isCorrect\":false,\"hintsUsed\":1}",
				"{\"sceneKey\":\"scene_2\",\"tryNo\":2,\"chosenOption\":\"a\",\"isCorrect\":true,\"hintsUsed\":1}"}) {
			mockMvc.perform(post("/api/attempts/" + attemptId + "/answers").with(csrf())
							.session(parent).contentType(MediaType.APPLICATION_JSON).content(answer))
					.andExpect(status().isCreated());
		}
		mockMvc.perform(post("/api/attempts/" + attemptId + "/complete").with(csrf())
						.session(parent).contentType(MediaType.APPLICATION_JSON)
						.content("{\"result\":\"completed\",\"score\":0.5}"))
				.andExpect(status().isOk());
		return childId;
	}

	private String joinNewGroup(MockHttpSession teacher, String name) throws Exception {
		return JsonPath.read(mockMvc.perform(post("/api/groups").with(csrf())
						.session(teacher).contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"%s\"}".formatted(name)))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.joinCode");
	}

	private void join(MockHttpSession parent, UUID childId, String joinCode) throws Exception {
		mockMvc.perform(post("/api/groups/join").with(csrf())
						.session(parent).contentType(MediaType.APPLICATION_JSON)
						.content("{\"joinCode\":\"%s\",\"childId\":\"%s\"}".formatted(joinCode, childId)))
				.andExpect(status().isOk());
	}

	private ResultActions deleteAccount(MockHttpSession session, String password) throws Exception {
		return mockMvc.perform(delete("/api/me").with(csrf())
				.session(session)
				.header("X-Forwarded-For", "ip-" + UUID.randomUUID())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"password\":\"%s\"}".formatted(password)));
	}

	private int count(String sql, Object... args) {
		return jdbc.queryForObject(sql, Integer.class, args);
	}

	// ---- export ---------------------------------------------------------------------------------

	@Test
	void childExportHasEverySectionAndNothingAboutOtherChildrenOrAdults() throws Exception {
		Account parent = user(Role.PARENT);
		MockHttpSession parentSession = login(parent.email());
		UUID childId = seededChildWithHistory(parentSession);
		mockMvc.perform(put("/api/children/" + childId + "/consents/style_personalization").with(csrf())
				.session(parentSession)).andExpect(status().isNoContent());
		mockMvc.perform(put("/api/children/" + childId + "/style").with(csrf()).session(parentSession)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"colorId\":\"blue\",\"worldId\":\"space\",\"moodId\":\"calm\"}")).andExpect(status().isOk());

		Account teacher = user(Role.TEACHER);
		String joinCode = joinNewGroup(login(teacher.email()), "Сонечко");
		join(parentSession, childId, joinCode);
		// Another family's child in the same group.
		Account otherParent = user(Role.PARENT);
		MockHttpSession otherSession = login(otherParent.email());
		join(otherSession, child(otherSession, "Чужа-Дитина-Марко"), joinCode);

		MvcResult result = mockMvc.perform(get("/api/children/" + childId + "/export").session(parentSession))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Disposition", "attachment; filename=\"kidlearn-child-" + childId + ".json\""))
				.andExpect(jsonPath("$.schemaVersion").value(1))
				.andExpect(jsonPath("$.exportedAt").exists())
				.andExpect(jsonPath("$.child.id").value(childId.toString()))
				.andExpect(jsonPath("$.child.birthYear").exists())
				.andExpect(jsonPath("$.consents[?(@.type == 'account')]").isNotEmpty())
				.andExpect(jsonPath("$.consents[?(@.type == 'style_personalization')]").isNotEmpty())
				.andExpect(jsonPath("$.groups[0].name").value("Сонечко"))
				.andExpect(jsonPath("$.attempts[0].lessonTitle").exists())
				.andExpect(jsonPath("$.attempts[0].score").value(0.5))
				.andExpect(jsonPath("$.attempts[0].answers.length()").value(2))
				.andExpect(jsonPath("$.attempts[0].answers[0].answeredAt").exists())
				.andExpect(jsonPath("$.attempts[0].answers[0].hintsUsed").value(1))
				.andExpect(jsonPath("$.playSettings.sessionLimitMinutes").value(15))
				.andExpect(jsonPath("$.stylePreferences.colorId").value("blue"))
				.andReturn();
		String json = result.getResponse().getContentAsString();
		assertThat(json).doesNotContain("Чужа-Дитина-Марко", teacher.email(), otherParent.email(), joinCode,
				parent.email());
		assertThat(result.getResponse().getContentType()).startsWith("application/json");
	}

	@Test
	void anotherParentsChildIs404AndATeacherIsForbidden() throws Exception {
		MockHttpSession owner = login(user(Role.PARENT).email());
		UUID childId = child(owner, "Своя");
		mockMvc.perform(get("/api/children/" + childId + "/export").session(login(user(Role.PARENT).email())))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/children/" + childId + "/export").session(login(user(Role.TEACHER).email())))
				.andExpect(status().isForbidden());
	}

	@Test
	void accountExportHasTheAccountItsConsentsAndEachChild() throws Exception {
		Account parent = user(Role.PARENT);
		MockHttpSession session = login(parent.email());
		UUID first = child(session, "Перша");
		UUID second = child(session, "Друга");
		mockMvc.perform(get("/api/me/export").session(session))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Disposition", "attachment; filename=\"kidlearn-account.json\""))
				.andExpect(jsonPath("$.account.email").value(parent.email()))
				.andExpect(jsonPath("$.account.role").value("PARENT"))
				.andExpect(jsonPath("$.consentsGiven.length()").value(2))
				.andExpect(jsonPath("$.children.length()").value(2))
				.andExpect(jsonPath("$.children[?(@.child.id == '%s')]".formatted(first)).isNotEmpty())
				.andExpect(jsonPath("$.children[?(@.child.id == '%s')]".formatted(second)).isNotEmpty());
	}

	// ---- account deletion -----------------------------------------------------------------------------

	@Test
	void wrongPasswordDeletesNothing() throws Exception {
		Account parent = user(Role.PARENT);
		MockHttpSession session = login(parent.email());
		child(session, "Лишається");
		deleteAccount(session, "wrong-password").andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"));
		assertThat(userRepository.existsById(parent.id())).isTrue();
		mockMvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk());
	}

	@Test
	void deletingTheAccountRemovesSoleChildrenEntirelyAndLeavesSharedOnesWithTheOtherParent() throws Exception {
		Account parent = user(Role.PARENT);
		MockHttpSession session = login(parent.email());
		UUID soleChild = seededChildWithHistory(session);
		UUID sharedChild = child(session, "Спільна");
		Account otherParent = user(Role.PARENT);
		// No product flow adds a second parent yet; the link is what such a flow would create.
		parentChildRepository.save(new ParentChild(otherParent.id(), sharedChild, "father", false));
		assertThat(count("SELECT count(*) FROM scene_answers sa JOIN lesson_attempts la ON la.id = sa.attempt_id WHERE la.child_id = ?", soleChild))
				.isEqualTo(2);

		mockMvc.perform(get("/api/me/deletion-preview").session(session))
				.andExpect(jsonPath("$.children[?(@.childId == '%s')].deletedCompletely".formatted(soleChild)).value(true))
				.andExpect(jsonPath("$.children[?(@.childId == '%s')].deletedCompletely".formatted(sharedChild)).value(false));

		deleteAccount(session, PASSWORD).andExpect(status().isNoContent());

		// The user is really gone, not soft-deleted; the session is over.
		assertThat(count("SELECT count(*) FROM users WHERE id = ?", parent.id())).isZero();
		mockMvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
		// The sole child and everything about them.
		assertThat(count("SELECT count(*) FROM children WHERE id = ?", soleChild)).isZero();
		assertThat(count("SELECT count(*) FROM lesson_attempts WHERE child_id = ?", soleChild)).isZero();
		assertThat(count("SELECT count(*) FROM consents WHERE child_id = ?", soleChild)).isZero();
		assertThat(count("SELECT count(*) FROM child_play_state WHERE child_id = ?", soleChild)).isZero();
		assertThat(count("SELECT count(*) FROM scene_answers sa WHERE NOT EXISTS (SELECT 1 FROM lesson_attempts la WHERE la.id = sa.attempt_id)"))
				.isZero();
		// The shared child stays with the other parent; the deleted parent's traces are de-identified.
		assertThat(count("SELECT count(*) FROM children WHERE id = ? AND created_by IS NULL", sharedChild)).isEqualTo(1);
		assertThat(count("SELECT count(*) FROM parent_child WHERE child_id = ?", sharedChild)).isEqualTo(1);
		assertThat(count("SELECT count(*) FROM parent_child WHERE child_id = ? AND parent_id = ?", sharedChild, otherParent.id())).isEqualTo(1);
		assertThat(count("SELECT count(*) FROM consents WHERE child_id = ? AND parent_id IS NULL", sharedChild)).isEqualTo(1);
		// The audit trail survives, with no actor.
		assertThat(count("SELECT count(*) FROM audit_log WHERE action = 'DELETE_ACCOUNT' AND target_id = ? AND actor_id IS NULL", parent.id()))
				.isEqualTo(1);
		assertThat(count("SELECT count(*) FROM audit_log WHERE action = 'DELETE_CHILD' AND target_id = ? AND actor_id IS NULL", soleChild))
				.isEqualTo(1);
		assertThat(count("SELECT count(*) FROM audit_log WHERE action = 'DELETE_CHILD' AND target_id = ?", sharedChild)).isZero();
	}

	@Test
	void teachersAndAdminsCantDeleteThemselves() throws Exception {
		Account teacher = user(Role.TEACHER);
		deleteAccount(login(teacher.email()), PASSWORD)
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("DELETION_VIA_ADMIN"))
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("адміністратор")));
		assertThat(userRepository.existsById(teacher.id())).isTrue();

		Account admin = user(Role.ADMIN);
		MockHttpSession adminSession = login(admin.email());
		ua.kidlearn.twofa.AdminTwoFactorTestSupport.completeSetup(mockMvc, adminSession, testClock.instant());
		deleteAccount(adminSession, PASSWORD)
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ADMIN_NOT_SELF_DELETABLE"));
		assertThat(userRepository.existsById(admin.id())).isTrue();
	}

	// ---- sessions --------------------------------------------------------------------------------

	@Test
	void signingOutEverywhereEndsEverySessionIncludingThisOne() throws Exception {
		Account parent = user(Role.PARENT);
		MockHttpSession first = login(parent.email());
		MockHttpSession second = login(parent.email());
		testClock.advance(Duration.ofSeconds(1));

		mockMvc.perform(post("/api/me/logout-everywhere").with(csrf()).session(first)).andExpect(status().isNoContent());

		mockMvc.perform(get("/api/auth/me").session(second)).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/auth/me").session(first)).andExpect(status().isUnauthorized());
		// A login afterwards is a fresh, valid session.
		testClock.advance(Duration.ofSeconds(1));
		mockMvc.perform(get("/api/auth/me").session(login(parent.email()))).andExpect(status().isOk());
	}

	@Test
	void changingThePasswordEndsOtherSessionsButNotThisOne() throws Exception {
		Account parent = user(Role.PARENT);
		MockHttpSession current = login(parent.email());
		MockHttpSession other = login(parent.email());
		testClock.advance(Duration.ofSeconds(1));

		mockMvc.perform(post("/api/me/password").with(csrf()).session(current)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"currentPassword\":\"%s\",\"newPassword\":\"short\"}".formatted(PASSWORD)))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/me/password").with(csrf()).session(current)
						.header("X-Forwarded-For", "ip-" + UUID.randomUUID())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"currentPassword\":\"wrong-one-123\",\"newPassword\":\"brand-new-pass-1\"}"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/me/password").with(csrf()).session(current)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"currentPassword\":\"%s\",\"newPassword\":\"brand-new-pass-1\"}".formatted(PASSWORD)))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/auth/me").session(current)).andExpect(status().isOk());
		mockMvc.perform(get("/api/auth/me").session(other)).andExpect(status().isUnauthorized());
		mockMvc.perform(formLogin().user(parent.email()).password(PASSWORD)).andExpect(header().string("Location", "/login?error"));
		testClock.advance(Duration.ofSeconds(1));
		mockMvc.perform(get("/api/auth/me").session(login(parent.email(), "brand-new-pass-1"))).andExpect(status().isOk());
	}

	// ---- retention ---------------------------------------------------------------------------------

	@Test
	void retentionDeletesOnlyOldDetailedHistoryAndKeepsTheAttempts() throws Exception {
		MockHttpSession oldFamily = login(user(Role.PARENT).email());
		UUID oldChild = seededChildWithHistory(oldFamily);
		MockHttpSession recentFamily = login(user(Role.PARENT).email());
		UUID recentChild = seededChildWithHistory(recentFamily);
		// Two years and a day ago, by the test clock.
		jdbc.update("UPDATE lesson_attempts SET started_at = ?, completed_at = ? WHERE child_id = ?",
				java.sql.Timestamp.from(testClock.instant().minus(Duration.ofDays(732))),
				java.sql.Timestamp.from(testClock.instant().minus(Duration.ofDays(731))), oldChild);

		Account staleUnverified = user(Role.PARENT);
		jdbc.update("UPDATE users SET email_verified_at = NULL, created_at = ? WHERE id = ?",
				java.sql.Timestamp.from(testClock.instant().minus(Duration.ofDays(31))), staleUnverified.id());
		Account freshUnverified = user(Role.PARENT);
		jdbc.update("UPDATE users SET email_verified_at = NULL WHERE id = ?", freshUnverified.id());
		UUID tokenOwner = user(Role.PARENT).id();
		jdbc.update("""
				INSERT INTO user_tokens (user_id, type, token_hash, expires_at, used_at, created_at)
				VALUES (?, 'password_reset', ?, ?, ?, ?), (?, 'password_reset', ?, ?, NULL, ?)""",
				tokenOwner, "old-" + UUID.randomUUID(), java.sql.Timestamp.from(testClock.instant().minus(Duration.ofDays(39))),
				java.sql.Timestamp.from(testClock.instant().minus(Duration.ofDays(40))),
				java.sql.Timestamp.from(testClock.instant().minus(Duration.ofDays(40))),
				tokenOwner, "live-" + UUID.randomUUID(), java.sql.Timestamp.from(testClock.instant().plus(Duration.ofHours(1))),
				java.sql.Timestamp.from(testClock.instant()));

		RetentionService.Result result = retentionService.purge();

		assertThat(result.sceneAnswers()).isGreaterThanOrEqualTo(2);
		String answersOf = "SELECT count(*) FROM scene_answers sa JOIN lesson_attempts la ON la.id = sa.attempt_id WHERE la.child_id = ?";
		assertThat(count(answersOf, oldChild)).isZero();
		assertThat(count(answersOf, recentChild)).isEqualTo(2);
		// The aggregate stays: the old attempt with its score.
		assertThat(count("SELECT count(*) FROM lesson_attempts WHERE child_id = ? AND score = 0.5", oldChild)).isEqualTo(1);
		assertThat(count("SELECT count(*) FROM users WHERE id = ?", staleUnverified.id())).isZero();
		assertThat(count("SELECT count(*) FROM users WHERE id = ?", freshUnverified.id())).isEqualTo(1);
		assertThat(count("SELECT count(*) FROM user_tokens WHERE user_id = ?", tokenOwner)).isEqualTo(1);
		assertThat(count("SELECT count(*) FROM user_tokens WHERE user_id = ? AND used_at IS NULL", tokenOwner)).isEqualTo(1);
	}

}
