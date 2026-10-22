package ua.kidlearn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.time.Year;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import ua.kidlearn.children.ParentChild;
import ua.kidlearn.children.ParentChildRepository;
import ua.kidlearn.scenario.ScenarioFixtures;
import ua.kidlearn.support.WebIntegrationTest;
import ua.kidlearn.twofa.AdminTwoFactorTestSupport;
import ua.kidlearn.users.Role;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

/**
 * The access matrix (QA round 1): every mapped endpoint, called as every kind of actor, with its
 * own and someone else's ids. Ownership is enforced as 404 (never 403 or 200), a wrong role as
 * 403, an admin session short of ELEVATED as 403 with the 2FA code, an anonymous or deactivated
 * session as 401, and a mutation without the CSRF token as 403. Refused responses must not echo
 * anything about other families (names, emails, join codes).
 *
 * <p>All deviations are collected and reported together, so one run shows the whole matrix.
 * Accounts are created through the repositories (no product flow creates a second admin or links
 * a second parent); everything else goes through the API. Owner calls that would destroy the
 * fixture (deleting, archiving, signing out) are only checked from the refusing side here — their
 * success paths have their own tests. Dev-only routes are covered by
 * DevRoutesAbsentOutsideDevProfileTest.
 */
@Transactional
class AccessMatrixTest extends WebIntegrationTest {

	private static final String PASSWORD = "supersecret1";

	enum Actor {
		ANON, PARENT_A, PARENT_B, SECOND_PARENT, TEACHER_1, TEACHER_2, TEACHER_DEACTIVATED,
		ADMIN_SETUP_REQUIRED, ADMIN_PENDING_VERIFICATION, ADMIN_ELEVATED
	}

	/** status 0 = "authorized": anything but 401/403/404 and below 500; -1 = not checked. */
	private record Expect(int status, String code) {

		static final Expect OK = new Expect(0, null);
		static final Expect SKIP = new Expect(-1, null);
		static final Expect UNAUTHORIZED = new Expect(401, null);
		static final Expect FORBIDDEN = new Expect(403, null);
		static final Expect NOT_FOUND = new Expect(404, null);
		static final Expect GONE = new Expect(410, null);
		static final Expect SETUP_REQUIRED = new Expect(403, "TWO_FACTOR_SETUP_REQUIRED");
		static final Expect VERIFICATION_REQUIRED = new Expect(403, "TWO_FACTOR_VERIFICATION_REQUIRED");

		boolean matches(MockHttpServletResponse response) throws Exception {
			int actual = response.getStatus();
			if (status == 0) {
				return actual < 500 && actual != 401 && actual != 403 && actual != 404;
			}
			return actual == status && (code == null || response.getContentAsString().contains(code));
		}

		@Override
		public String toString() {
			return status == 0 ? "authorized" : status + (code == null ? "" : " " + code);
		}

	}

	private record Case(HttpMethod method, String path, String body, Map<Actor, Expect> expected) {

		boolean mutation() {
			return method != HttpMethod.GET;
		}

	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ParentChildRepository parentChildRepository;

	@Autowired
	@Qualifier("requestMappingHandlerMapping")
	private RequestMappingHandlerMapping handlerMapping;

	private final Map<Actor, MockHttpSession> sessions = new EnumMap<>(Actor.class);

	private final List<String> secrets = new ArrayList<>();

	private final Map<UUID, String> childNames = new HashMap<>();

	private final String run = UUID.randomUUID().toString().substring(0, 8);

	private UUID childA1;

	private UUID childB1;

	private UUID sharedChild;

	private UUID groupG1;

	private UUID groupG2;

	private String joinCodeG2;

	private UUID lessonId;

	private UUID publishedForG1;

	private UUID publishedForG2;

	private UUID draftVersion;

	private UUID attemptA1;

	private UUID assignmentG1;

	private UUID teacher1Id;

	// ---- fixture --------------------------------------------------------------------------------

	@BeforeEach
	void buildFixture() throws Exception {
		MockHttpSession admin = login(account(Role.ADMIN, "admin"));
		AdminTwoFactorTestSupport.completeSetup(mockMvc, admin);
		sessions.put(Actor.ADMIN_ELEVATED, admin);
		String adminEmail = secrets.getLast();
		sessions.put(Actor.ADMIN_PENDING_VERIFICATION, login(adminEmail));
		sessions.put(Actor.ADMIN_SETUP_REQUIRED, login(account(Role.ADMIN, "admin-new")));

		String teacher1 = account(Role.TEACHER, "teacher1");
		teacher1Id = userRepository.findActiveByEmail(teacher1).orElseThrow().getId();
		sessions.put(Actor.TEACHER_1, login(teacher1));
		sessions.put(Actor.TEACHER_2, login(account(Role.TEACHER, "teacher2")));
		String teacher3 = account(Role.TEACHER, "teacher3");
		sessions.put(Actor.TEACHER_DEACTIVATED, login(teacher3));

		sessions.put(Actor.PARENT_A, login(account(Role.PARENT, "parentA")));
		sessions.put(Actor.PARENT_B, login(account(Role.PARENT, "parentB")));
		String secondParent = account(Role.PARENT, "parent2");
		sessions.put(Actor.SECOND_PARENT, login(secondParent));

		childA1 = child(Actor.PARENT_A, "Дитина-А1-" + run);
		sharedChild = child(Actor.PARENT_A, "Спільна-Дитина-" + run);
		parentChildRepository.saveAndFlush(new ParentChild(
				userRepository.findActiveByEmail(secondParent).orElseThrow().getId(), sharedChild, "father", false));
		childB1 = child(Actor.PARENT_B, "Дитина-Б1-" + run);

		String joinCodeG1 = JsonPath.read(ok(Actor.TEACHER_1, HttpMethod.POST, "/api/groups", "{\"name\":\"Г1-%s\"}".formatted(run)), "$.joinCode");
		joinCodeG2 = JsonPath.read(ok(Actor.TEACHER_2, HttpMethod.POST, "/api/groups", "{\"name\":\"Г2-%s\"}".formatted(run)), "$.joinCode");
		secrets.add(joinCodeG1);
		secrets.add(joinCodeG2);
		groupG1 = groupId(Actor.TEACHER_1);
		groupG2 = groupId(Actor.TEACHER_2);
		join(Actor.PARENT_A, joinCodeG1, childA1);
		join(Actor.PARENT_B, joinCodeG2, childB1);

		lessonId = UUID.fromString(JsonPath.read(ok(Actor.ADMIN_ELEVATED, HttpMethod.POST, "/api/admin/lessons",
				"{\"moduleCode\":\"safety\",\"title\":\"Матриця %s\"}".formatted(run)), "$.id"));
		publishedForG1 = publishedVersion();
		publishedForG2 = publishedVersion();
		draftVersion = UUID.fromString(JsonPath.read(ok(Actor.ADMIN_ELEVATED, HttpMethod.POST,
				"/api/admin/lessons/" + lessonId + "/versions",
				"{\"scenario\":" + ScenarioFixtures.VALID_MINIMAL_SCENARIO + ",\"generatedBy\":\"human\"}"), "$.id"));

		assignmentG1 = UUID.fromString(JsonPath.read(ok(Actor.TEACHER_1, HttpMethod.POST, "/api/assignments",
				"{\"lessonVersionId\":\"%s\",\"groupId\":\"%s\"}".formatted(publishedForG1, groupG1)), "$.id"));
		ok(Actor.TEACHER_2, HttpMethod.POST, "/api/assignments",
				"{\"lessonVersionId\":\"%s\",\"groupId\":\"%s\"}".formatted(publishedForG2, groupG2));
		attemptA1 = UUID.fromString(JsonPath.read(ok(Actor.PARENT_A, HttpMethod.POST, "/api/children/" + childA1 + "/attempts",
				"{\"lessonVersionId\":\"%s\"}".formatted(publishedForG1)), "$.attemptId"));

		UUID teacher3Id = userRepository.findActiveByEmail(teacher3).orElseThrow().getId();
		ok(Actor.ADMIN_ELEVATED, HttpMethod.POST, "/api/admin/teachers/" + teacher3Id + "/deactivate", null);
	}

	private String account(Role role, String label) {
		String email = "matrix-" + label + "-" + UUID.randomUUID() + "@example.test";
		User user = new User(email, passwordEncoder.encode(PASSWORD), role, "Matrix " + label, "uk");
		user.markEmailVerified(Instant.now());
		userRepository.saveAndFlush(user);
		secrets.add(email);
		return email;
	}

	private MockHttpSession login(String email) throws Exception {
		MvcResult result = mockMvc.perform(formLogin().user(email).password(PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private UUID child(Actor parent, String name) throws Exception {
		secrets.add(name);
		UUID id = UUID.fromString(JsonPath.read(ok(parent, HttpMethod.POST, "/api/children",
				"{\"displayName\":\"%s\",\"birthYear\":%d,\"relation\":\"mother\"}".formatted(name, Year.now().getValue() - 5)), "$.id"));
		ok(parent, HttpMethod.POST, "/api/children/" + id + "/consent", null);
		childNames.put(id, name);
		return id;
	}

	private UUID groupId(Actor teacher) throws Exception {
		return UUID.fromString(JsonPath.read(ok(teacher, HttpMethod.GET, "/api/groups", null), "$[0].id"));
	}

	private void join(Actor parent, String joinCode, UUID childId) throws Exception {
		ok(parent, HttpMethod.POST, "/api/groups/join", "{\"joinCode\":\"%s\",\"childId\":\"%s\"}".formatted(joinCode, childId));
	}

	private UUID publishedVersion() throws Exception {
		UUID id = UUID.fromString(JsonPath.read(ok(Actor.ADMIN_ELEVATED, HttpMethod.POST,
				"/api/admin/lessons/" + lessonId + "/generate", GENERATION_BODY), "$.versionId"));
		ok(Actor.ADMIN_ELEVATED, HttpMethod.POST, "/api/admin/lesson-versions/" + id + "/approve", null);
		ok(Actor.ADMIN_ELEVATED, HttpMethod.POST, "/api/admin/lesson-versions/" + id + "/publish", null);
		return id;
	}

	private static final String GENERATION_BODY = """
			{"topic":"road_safety","patternIds":["1.6"],"ageBand":"age_5_6",
			 "title":"Matrix","learningGoal":"Look both ways before crossing"}""";

	private String ok(Actor actor, HttpMethod method, String path, String body) throws Exception {
		MockHttpServletResponse response = perform(actor, method, path, body, true);
		assertThat(response.getStatus()).as("%s %s as %s: %s", method, path, actor, response.getContentAsString())
				.isBetween(200, 299);
		return response.getContentAsString();
	}

	private MockHttpServletResponse perform(Actor actor, HttpMethod method, String path, String body, boolean withCsrf)
			throws Exception {
		MockHttpServletRequestBuilder builder = request(method, path)
				// Every call from its own address: the per-IP limits are not what this test is about.
				.header("X-Forwarded-For", "ip-" + UUID.randomUUID());
		if (actor != Actor.ANON) {
			builder.session(sessions.get(actor));
		}
		if (withCsrf) {
			builder.with(csrf());
		}
		if (body != null) {
			builder.contentType(MediaType.APPLICATION_JSON).content(body);
		}
		return mockMvc.perform(builder).andReturn().getResponse();
	}

	// ---- expectations ---------------------------------------------------------------------------

	/** What every endpoint shares: anonymous and deactivated → 401, half-signed-in admins → 2FA. */
	private static Map<Actor, Expect> base() {
		Map<Actor, Expect> expected = new EnumMap<>(Actor.class);
		expected.put(Actor.ANON, Expect.UNAUTHORIZED);
		expected.put(Actor.TEACHER_DEACTIVATED, Expect.UNAUTHORIZED);
		expected.put(Actor.ADMIN_SETUP_REQUIRED, Expect.SETUP_REQUIRED);
		expected.put(Actor.ADMIN_PENDING_VERIFICATION, Expect.VERIFICATION_REQUIRED);
		return expected;
	}

	/** A parent-only endpoint about one child: owners pass, other parents get 404, other roles 403. */
	private static Map<Actor, Expect> child(Expect owner, Actor... owners) {
		Map<Actor, Expect> expected = base();
		for (Actor parent : List.of(Actor.PARENT_A, Actor.PARENT_B, Actor.SECOND_PARENT)) {
			expected.put(parent, List.of(owners).contains(parent) ? owner : Expect.NOT_FOUND);
		}
		expected.put(Actor.TEACHER_1, Expect.FORBIDDEN);
		expected.put(Actor.TEACHER_2, Expect.FORBIDDEN);
		expected.put(Actor.ADMIN_ELEVATED, Expect.FORBIDDEN);
		return expected;
	}

	/** A parent-only endpoint without a child in the path. */
	private static Map<Actor, Expect> parent(Expect each) {
		return child(each, Actor.PARENT_A, Actor.PARENT_B, Actor.SECOND_PARENT);
	}

	/** A teacher-only endpoint about group G1 (TEACHER_1's): TEACHER_2 gets 404, other roles 403. */
	private static Map<Actor, Expect> group(Expect owner, Expect otherTeacher) {
		Map<Actor, Expect> expected = base();
		expected.put(Actor.PARENT_A, Expect.FORBIDDEN);
		expected.put(Actor.PARENT_B, Expect.FORBIDDEN);
		expected.put(Actor.SECOND_PARENT, Expect.FORBIDDEN);
		expected.put(Actor.TEACHER_1, owner);
		expected.put(Actor.TEACHER_2, otherTeacher);
		expected.put(Actor.ADMIN_ELEVATED, Expect.FORBIDDEN);
		return expected;
	}

	private static Map<Actor, Expect> admin(Expect elevated) {
		Map<Actor, Expect> expected = group(Expect.FORBIDDEN, Expect.FORBIDDEN);
		expected.put(Actor.ADMIN_ELEVATED, elevated);
		return expected;
	}

	private static Map<Actor, Expect> anyone(Expect each) {
		Map<Actor, Expect> expected = base();
		for (Actor actor : List.of(Actor.PARENT_A, Actor.PARENT_B, Actor.SECOND_PARENT, Actor.TEACHER_1,
				Actor.TEACHER_2, Actor.ADMIN_ELEVATED)) {
			expected.put(actor, each);
		}
		return expected;
	}

	/** permitAll routes: only the anonymous call is checked (a signed-in caller is out of scope). */
	private static Map<Actor, Expect> open(Expect anonymous) {
		Map<Actor, Expect> expected = new EnumMap<>(Actor.class);
		expected.put(Actor.ANON, anonymous);
		return expected;
	}

	private static Map<Actor, Expect> with(Map<Actor, Expect> expected, Actor actor, Expect expect) {
		expected.put(actor, expect);
		return expected;
	}

	private List<Case> cases() {
		String a1 = "/api/children/" + childA1;
		String g1 = "/api/groups/" + groupG1;
		String version = "/api/admin/lesson-versions/";
		String style = "{\"colorId\":\"blue\",\"worldId\":\"space\",\"moodId\":\"calm\"}";
		String answer = "{\"sceneKey\":\"scene_2\",\"tryNo\":1,\"chosenOption\":\"a\",\"isCorrect\":true,\"hintsUsed\":0}";
		String newChild = "{\"displayName\":\"Нова\",\"birthYear\":%d,\"relation\":\"mother\"}".formatted(Year.now().getValue() - 5);
		UUID nobody = UUID.randomUUID();
		return List.of(
				// --- auth / public
				new Case(HttpMethod.POST, "/api/auth/register",
						"{\"email\":\"matrix-new-%s@example.test\",\"password\":\"%s\",\"displayName\":\"Нова\"}".formatted(UUID.randomUUID(), PASSWORD),
						open(Expect.OK)),
				new Case(HttpMethod.POST, "/api/auth/verify-email", "{\"token\":\"not-a-token\"}", open(Expect.OK)),
				new Case(HttpMethod.POST, "/api/auth/forgot-password", "{\"email\":\"nobody@example.test\"}", open(Expect.OK)),
				new Case(HttpMethod.POST, "/api/auth/reset-password", "{\"token\":\"not-a-token\",\"newPassword\":\"" + PASSWORD + "\"}", open(Expect.OK)),
				new Case(HttpMethod.POST, "/api/auth/accept-invite", "{\"token\":\"not-a-token\",\"password\":\"" + PASSWORD + "\"}", open(Expect.OK)),
				new Case(HttpMethod.POST, "/api/bootstrap/admin",
						"{\"token\":\"%s\",\"email\":\"matrix-boot@example.test\",\"password\":\"%s\",\"displayName\":\"X\"}".formatted(BOOTSTRAP_TOKEN, PASSWORD),
						open(Expect.GONE)),
				new Case(HttpMethod.GET, "/api/auth/me", null, with(anyone(Expect.OK), Actor.ADMIN_SETUP_REQUIRED, Expect.OK)),
				new Case(HttpMethod.POST, "/api/auth/resend-verification", null, anyone(Expect.OK)),
				new Case(HttpMethod.POST, "/api/auth/reauthenticate", "{\"password\":\"wrong\"}", anyone(Expect.SKIP)),
				// --- account
				new Case(HttpMethod.GET, "/api/me/export", null, anyone(Expect.OK)),
				new Case(HttpMethod.GET, "/api/me/deletion-preview", null, parent(Expect.OK)),
				new Case(HttpMethod.POST, "/api/me/password", "{\"currentPassword\":\"wrong-password\",\"newPassword\":\"" + PASSWORD + "\"}", anyone(Expect.SKIP)),
				new Case(HttpMethod.POST, "/api/me/logout-everywhere", null, anyone(Expect.SKIP)),
				new Case(HttpMethod.DELETE, "/api/me", "{\"password\":\"wrong-password\"}", anyone(Expect.SKIP)),
				// --- shared content
				new Case(HttpMethod.GET, "/api/catalog/lessons", null, anyone(Expect.OK)),
				new Case(HttpMethod.GET, "/api/audio/" + "0".repeat(64), null, anyone(Expect.NOT_FOUND)),
				new Case(HttpMethod.GET, "/api/kid/shell-lines", null, parent(Expect.OK)),
				// --- children (parent A owns A1)
				new Case(HttpMethod.GET, "/api/children", null, parent(Expect.OK)),
				new Case(HttpMethod.POST, "/api/children", newChild, parent(Expect.OK)),
				new Case(HttpMethod.GET, a1, null, child(Expect.OK, Actor.PARENT_A)),
				new Case(HttpMethod.POST, a1 + "/consent", null, child(Expect.OK, Actor.PARENT_A)),
				new Case(HttpMethod.GET, a1 + "/consents", null, child(Expect.OK, Actor.PARENT_A)),
				new Case(HttpMethod.PUT, a1 + "/consents/style_personalization", null, child(Expect.OK, Actor.PARENT_A)),
				new Case(HttpMethod.GET, a1 + "/style", null, child(Expect.OK, Actor.PARENT_A)),
				new Case(HttpMethod.PUT, a1 + "/style", style, child(Expect.OK, Actor.PARENT_A)),
				new Case(HttpMethod.POST, a1 + "/style/seen", null, child(Expect.OK, Actor.PARENT_A)),
				new Case(HttpMethod.DELETE, a1 + "/consents/style_personalization", null, child(Expect.OK, Actor.PARENT_A)),
				new Case(HttpMethod.GET, a1 + "/export", null, child(Expect.OK, Actor.PARENT_A)),
				new Case(HttpMethod.GET, a1 + "/groups", null, child(Expect.OK, Actor.PARENT_A)),
				new Case(HttpMethod.GET, a1 + "/history", null, child(Expect.OK, Actor.PARENT_A)),
				new Case(HttpMethod.GET, a1 + "/available-lessons", null, child(Expect.OK, Actor.PARENT_A)),
				new Case(HttpMethod.GET, a1 + "/stickers", null, child(Expect.OK, Actor.PARENT_A)),
				new Case(HttpMethod.GET, a1 + "/play-state", null, child(Expect.OK, Actor.PARENT_A)),
				new Case(HttpMethod.PUT, a1 + "/play-settings", "{\"sessionLimitMinutes\":20}", child(Expect.OK, Actor.PARENT_A)),
				new Case(HttpMethod.POST, a1 + "/play-state/wake", null, child(Expect.OK, Actor.PARENT_A)),
				new Case(HttpMethod.GET, a1 + "/lessons/" + publishedForG1 + "/scenario", null, child(Expect.OK, Actor.PARENT_A)),
				new Case(HttpMethod.POST, a1 + "/attempts", "{\"lessonVersionId\":\"%s\"}".formatted(publishedForG1), child(Expect.OK, Actor.PARENT_A)),
				new Case(HttpMethod.POST, "/api/attempts/" + attemptA1 + "/answers", answer, child(Expect.OK, Actor.PARENT_A)),
				new Case(HttpMethod.POST, "/api/attempts/" + attemptA1 + "/complete", "{\"result\":\"completed\",\"score\":1}", child(Expect.OK, Actor.PARENT_A)),
				new Case(HttpMethod.POST, "/api/groups/join", "{\"joinCode\":\"%s\",\"childId\":\"%s\"}".formatted(joinCodeG2, childA1), child(Expect.SKIP, Actor.PARENT_A)),
				new Case(HttpMethod.DELETE, a1 + "/groups/" + groupG1, null, child(Expect.SKIP, Actor.PARENT_A)),
				new Case(HttpMethod.DELETE, a1, null, child(Expect.SKIP, Actor.PARENT_A)),
				// a lesson version the child has no access to: assigned only to another group / unpublished
				new Case(HttpMethod.GET, a1 + "/lessons/" + publishedForG2 + "/scenario", null, child(Expect.NOT_FOUND, Actor.PARENT_A)),
				new Case(HttpMethod.POST, a1 + "/attempts", "{\"lessonVersionId\":\"%s\"}".formatted(publishedForG2), child(Expect.NOT_FOUND, Actor.PARENT_A)),
				new Case(HttpMethod.POST, a1 + "/attempts", "{\"lessonVersionId\":\"%s\"}".formatted(draftVersion), child(Expect.NOT_FOUND, Actor.PARENT_A)),
				// the shared child: both of its parents, nobody else
				new Case(HttpMethod.GET, "/api/children/" + sharedChild, null, child(Expect.OK, Actor.PARENT_A, Actor.SECOND_PARENT)),
				new Case(HttpMethod.GET, "/api/children/" + sharedChild + "/export", null, child(Expect.OK, Actor.PARENT_A, Actor.SECOND_PARENT)),
				new Case(HttpMethod.GET, "/api/children/" + sharedChild + "/history", null, child(Expect.OK, Actor.PARENT_A, Actor.SECOND_PARENT)),
				// --- teachers (T1 owns G1 with A1; B1 is in T2's G2)
				new Case(HttpMethod.GET, "/api/groups", null, group(Expect.OK, Expect.OK)),
				new Case(HttpMethod.POST, "/api/groups", "{\"name\":\"Нова\"}", group(Expect.OK, Expect.OK)),
				new Case(HttpMethod.GET, g1 + "/members", null, group(Expect.OK, Expect.NOT_FOUND)),
				new Case(HttpMethod.GET, g1 + "/results", null, group(Expect.OK, Expect.NOT_FOUND)),
				new Case(HttpMethod.POST, g1 + "/regenerate-code", null, group(Expect.OK, Expect.NOT_FOUND)),
				new Case(HttpMethod.GET, "/api/assignments?groupId=" + groupG1, null, group(Expect.OK, Expect.NOT_FOUND)),
				new Case(HttpMethod.POST, "/api/assignments", "{\"lessonVersionId\":\"%s\",\"groupId\":\"%s\"}".formatted(publishedForG2, groupG1), group(Expect.OK, Expect.NOT_FOUND)),
				new Case(HttpMethod.POST, "/api/assignments", "{\"lessonVersionId\":\"%s\",\"childId\":\"%s\"}".formatted(publishedForG2, childA1), group(Expect.OK, Expect.NOT_FOUND)),
				new Case(HttpMethod.POST, "/api/assignments", "{\"lessonVersionId\":\"%s\",\"childId\":\"%s\"}".formatted(publishedForG1, childB1), group(Expect.NOT_FOUND, Expect.SKIP)),
				// idempotent within the teacher's own group: 204 for any child id, so nothing to learn from it
				new Case(HttpMethod.DELETE, g1 + "/members/" + childB1, null, group(Expect.OK, Expect.NOT_FOUND)),
				new Case(HttpMethod.DELETE, g1 + "/members/" + childA1, null, group(Expect.SKIP, Expect.NOT_FOUND)),
				new Case(HttpMethod.DELETE, "/api/assignments/" + assignmentG1, null, group(Expect.SKIP, Expect.NOT_FOUND)),
				new Case(HttpMethod.POST, g1 + "/archive", null, group(Expect.SKIP, Expect.NOT_FOUND)),
				// --- admin
				new Case(HttpMethod.GET, "/api/admin/ping", null, admin(Expect.OK)),
				new Case(HttpMethod.GET, "/api/admin/audit?limit=5&offset=0", null, admin(Expect.OK)),
				new Case(HttpMethod.GET, "/api/admin/lessons", null, admin(Expect.OK)),
				new Case(HttpMethod.POST, "/api/admin/lessons", "{\"moduleCode\":\"safety\",\"title\":\"Новий\"}", admin(Expect.OK)),
				new Case(HttpMethod.POST, "/api/admin/lessons/" + lessonId + "/versions",
						"{\"scenario\":" + ScenarioFixtures.VALID_MINIMAL_SCENARIO + ",\"generatedBy\":\"human\"}", admin(Expect.OK)),
				new Case(HttpMethod.POST, "/api/admin/lessons/" + lessonId + "/generate", GENERATION_BODY, admin(Expect.OK)),
				new Case(HttpMethod.POST, "/api/admin/lessons/" + nobody + "/generate", GENERATION_BODY, admin(Expect.NOT_FOUND)),
				new Case(HttpMethod.GET, "/api/admin/lesson-versions?status=draft", null, admin(Expect.OK)),
				new Case(HttpMethod.GET, version + publishedForG1, null, admin(Expect.OK)),
				new Case(HttpMethod.GET, version + nobody, null, admin(Expect.NOT_FOUND)),
				new Case(HttpMethod.POST, version + draftVersion + "/approve", null, admin(Expect.OK)),
				new Case(HttpMethod.POST, version + draftVersion + "/publish", null, admin(Expect.OK)),
				new Case(HttpMethod.POST, version + draftVersion + "/voice", null, admin(Expect.OK)),
				new Case(HttpMethod.POST, version + draftVersion + "/reject", "{\"reason\":\"QA\"}", admin(Expect.OK)),
				new Case(HttpMethod.POST, "/api/admin/kid-shell-lines/voice", null, admin(Expect.SKIP)),
				new Case(HttpMethod.GET, "/api/admin/teachers", null, admin(Expect.OK)),
				new Case(HttpMethod.POST, "/api/admin/teachers",
						"{\"email\":\"matrix-invite-%s@example.test\",\"displayName\":\"Нова\"}".formatted(UUID.randomUUID()), admin(Expect.OK)),
				new Case(HttpMethod.POST, "/api/admin/teachers/" + teacher1Id + "/resend-invite", null, admin(Expect.OK)),
				new Case(HttpMethod.POST, "/api/admin/teachers/" + nobody + "/deactivate", null, admin(Expect.NOT_FOUND)),
				new Case(HttpMethod.GET, "/api/admin/themes", null, admin(Expect.OK)),
				new Case(HttpMethod.GET, "/api/admin/themes/" + nobody, null, admin(Expect.NOT_FOUND)),
				new Case(HttpMethod.POST, "/api/admin/themes/" + nobody + "/approve", null, admin(Expect.NOT_FOUND)),
				new Case(HttpMethod.POST, "/api/admin/themes/" + nobody + "/reject", "{\"reason\":\"QA\"}", admin(Expect.NOT_FOUND)),
				new Case(HttpMethod.POST, "/api/admin/themes/regenerate", "{\"preferenceKey\":\"no-such-key\"}", admin(Expect.NOT_FOUND)),
				// 2FA itself: each half-signed-in state is TwoFactorFlowTest's subject
				new Case(HttpMethod.POST, "/api/admin/2fa/setup", null,
						with(with(admin(Expect.SKIP), Actor.ADMIN_SETUP_REQUIRED, Expect.SKIP), Actor.ADMIN_PENDING_VERIFICATION, Expect.SKIP)),
				new Case(HttpMethod.POST, "/api/admin/2fa/enable", "{\"code\":\"000000\"}",
						with(with(admin(Expect.SKIP), Actor.ADMIN_SETUP_REQUIRED, Expect.SKIP), Actor.ADMIN_PENDING_VERIFICATION, Expect.SKIP)),
				new Case(HttpMethod.POST, "/api/admin/2fa/verify", "{\"code\":\"000000\"}",
						with(with(admin(Expect.SKIP), Actor.ADMIN_SETUP_REQUIRED, Expect.SKIP), Actor.ADMIN_PENDING_VERIFICATION, Expect.SKIP)));
	}

	// ---- the matrix -----------------------------------------------------------------------------

	@Test
	void everyEndpointAsEveryActor() throws Exception {
		List<String> deviations = new ArrayList<>();
		int checked = 0;
		for (Case c : cases()) {
			for (Map.Entry<Actor, Expect> e : c.expected().entrySet()) {
				if (e.getValue() == Expect.SKIP) {
					continue;
				}
				MockHttpServletResponse response = perform(e.getKey(), c.method(), c.path(), c.body(), true);
				checked++;
				if (!e.getValue().matches(response)) {
					deviations.add("%s %s as %s: expected %s, got %d %s".formatted(c.method(), c.path(), e.getKey(),
							e.getValue(), response.getStatus(), abbreviate(response.getContentAsString())));
				}
				if (e.getValue().status() > 0) {
					String body = response.getContentAsString();
					for (String secret : secrets) {
						if (body.contains(secret)) {
							deviations.add("%s %s as %s: refusal leaks '%s'".formatted(c.method(), c.path(), e.getKey(), secret));
						}
					}
				}
			}
		}
		assertThat(deviations).as("access-matrix deviations (%d checks)", checked).isEmpty();
		assertThat(checked).isGreaterThan(500);
	}

	@Test
	void everyMutationWithoutTheCsrfTokenIs403() throws Exception {
		List<String> deviations = new ArrayList<>();
		for (Case c : cases()) {
			if (!c.mutation()) {
				continue;
			}
			// The caller that would otherwise be let through (or the anonymous one, on open routes).
			Actor caller = List.of(Actor.PARENT_A, Actor.TEACHER_1, Actor.ADMIN_ELEVATED, Actor.ANON).stream()
					.filter(a -> c.expected().containsKey(a) && c.expected().get(a).status() <= 0)
					.findFirst().orElse(Actor.PARENT_A);
			MockHttpServletResponse response = perform(caller, c.method(), c.path(), c.body(), false);
			if (response.getStatus() != 403) {
				deviations.add("%s %s as %s without CSRF: got %d".formatted(c.method(), c.path(), caller, response.getStatus()));
			}
		}
		for (String path : List.of("/login", "/logout")) {
			int status = mockMvc.perform(request(HttpMethod.POST, path).session(sessions.get(Actor.PARENT_A)))
					.andReturn().getResponse().getStatus();
			if (status != 403) {
				deviations.add("POST %s without CSRF: got %d".formatted(path, status));
			}
		}
		assertThat(deviations).isEmpty();
	}

	@Test
	void everyMappedEndpointIsInTheMatrix() throws Exception {
		List<String> covered = cases().stream()
				.map(c -> c.method() + " " + c.path().replaceAll("\\?.*", "")
						.replaceAll("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "{}")
						.replaceAll("0{64}", "{}")
						.replace("style_personalization", "{}"))
				.distinct().toList();
		List<String> missing = new ArrayList<>();
		for (var info : handlerMapping.getHandlerMethods().keySet()) {
			for (String pattern : info.getPatternValues()) {
				if (pattern.startsWith("/api/dev/") || !pattern.startsWith("/api/")) {
					continue;
				}
				for (var method : info.getMethodsCondition().getMethods()) {
					String key = method + " " + pattern.replaceAll("\\{[^}]+}", "{}");
					if (!covered.contains(key)) {
						missing.add(key);
					}
				}
			}
		}
		assertThat(missing).as("endpoints with no row in the access matrix").isEmpty();
	}

	@Test
	void responsesCarryNoDataBeyondTheCallersReach() throws Exception {
		String parentEmails = String.join("|", secrets.stream().filter(e -> e.contains("-parent")).toList());
		// A teacher sees the children of their groups, never the adults behind them.
		for (String path : List.of("/api/groups", "/api/groups/" + groupG1 + "/members", "/api/groups/" + groupG1 + "/results",
				"/api/assignments?groupId=" + groupG1)) {
			assertThat(ok(Actor.TEACHER_1, HttpMethod.GET, path, null)).as(path)
					.doesNotContainPattern(parentEmails)
					.doesNotContain(nameOf(childB1));
		}
		// A parent's lists hold their own children only.
		assertThat(ok(Actor.PARENT_A, HttpMethod.GET, "/api/children", null))
				.contains(nameOf(childA1), nameOf(sharedChild)).doesNotContain(nameOf(childB1));
		assertThat(ok(Actor.SECOND_PARENT, HttpMethod.GET, "/api/children", null))
				.contains(nameOf(sharedChild)).doesNotContain(nameOf(childA1), nameOf(childB1));
		// Exports: nothing about another family, the teacher, the co-parent's account or join codes.
		String accountExport = ok(Actor.PARENT_A, HttpMethod.GET, "/api/me/export", null);
		assertThat(accountExport).doesNotContain(nameOf(childB1), joinCodeG2);
		for (String secret : secrets) {
			if (!secret.contains("-parentA-") && secret.contains("@")) {
				assertThat(accountExport).as("account export").doesNotContain(secret);
			}
		}
		String sharedExport = ok(Actor.SECOND_PARENT, HttpMethod.GET, "/api/children/" + sharedChild + "/export", null);
		assertThat(sharedExport).doesNotContain(nameOf(childA1)).doesNotContainPattern(parentEmails);
		// Group membership as the parent sees it: the group's name, not the teacher or other children.
		assertThat(ok(Actor.PARENT_A, HttpMethod.GET, "/api/children/" + childA1 + "/groups", null))
				.doesNotContainPattern(String.join("|", secrets.stream().filter(e -> e.contains("-teacher")).toList()));
	}

	@Test
	void loginAndPasswordResetDontTellWhetherAnAccountExists() throws Exception {
		String existing = secrets.stream().filter(e -> e.contains("-parentB-")).findFirst().orElseThrow();
		MockHttpServletResponse wrongPassword = mockMvc.perform(formLogin().user(existing).password("wrong-password-1")).andReturn().getResponse();
		MockHttpServletResponse unknown = mockMvc.perform(formLogin().user("matrix-nobody-" + run + "@example.test")
				.password("wrong-password-1")).andReturn().getResponse();
		assertThat(wrongPassword.getStatus()).isEqualTo(unknown.getStatus());
		assertThat(wrongPassword.getContentAsString()).isEqualTo(unknown.getContentAsString());
		assertThat(wrongPassword.getRedirectedUrl()).isEqualTo(unknown.getRedirectedUrl());

		MockHttpServletResponse forgotExisting = perform(Actor.ANON, HttpMethod.POST, "/api/auth/forgot-password",
				"{\"email\":\"%s\"}".formatted(existing), true);
		MockHttpServletResponse forgotUnknown = perform(Actor.ANON, HttpMethod.POST, "/api/auth/forgot-password",
				"{\"email\":\"matrix-nobody-%s@example.test\"}".formatted(run), true);
		assertThat(forgotExisting.getStatus()).isEqualTo(forgotUnknown.getStatus());
		assertThat(forgotExisting.getContentAsString()).isEqualTo(forgotUnknown.getContentAsString());
	}

	private String nameOf(UUID childId) {
		return childNames.get(childId);
	}

	private static String abbreviate(String body) {
		return body.length() > 160 ? body.substring(0, 160) + "…" : body;
	}

}
