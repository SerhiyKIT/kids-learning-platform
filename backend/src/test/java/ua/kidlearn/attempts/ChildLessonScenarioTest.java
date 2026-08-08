package ua.kidlearn.attempts;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Year;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import ua.kidlearn.lessons.LessonAssignment;
import ua.kidlearn.lessons.LessonAssignmentRepository;
import ua.kidlearn.users.Role;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

/**
 * GET /api/children/{childId}/lessons/{lessonVersionId}/scenario — the child-facing "fetch
 * playable scenario" endpoint (parents only; the version must be published AND assigned to the
 * child, directly or via a group).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class ChildLessonScenarioTest {

	private static final String PASSWORD = "supersecret1";

	private static final String GENERATION_REQUEST_BODY = """
			{"topic":"road_safety","patternIds":["1.6"],"ageBand":"age_5_6",
			 "learningGoal":"Look both ways before crossing"}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private LessonAssignmentRepository lessonAssignmentRepository;

	private record Fixture(UUID versionId, UUID groupId, String joinCode, MockHttpSession parentSession,
			UUID childId) {
	}

	private static String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@example.test";
	}

	private User registerAdmin(String email) {
		return userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), Role.ADMIN, "Admin", "uk"));
	}

	private void registerTeacher(String email) {
		userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), Role.TEACHER, "Teacher", "uk"));
	}

	private User registerParent(String email) throws Exception {
		String body = "{\"email\":\"%s\",\"password\":\"%s\",\"displayName\":\"Test\"}".formatted(email, PASSWORD);
		mockMvc.perform(post("/api/auth/register").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated());
		return userRepository.findByEmailAndDeletedAtIsNull(email).orElseThrow();
	}

	private void registerVerifiedParent(String email) throws Exception {
		User user = registerParent(email);
		user.markEmailVerified();
		userRepository.save(user);
	}

	private MockHttpSession login(String email) throws Exception {
		MvcResult result = mockMvc.perform(formLogin().user(email).password(PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private UUID createLesson(MockHttpSession adminSession, String title) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/admin/lessons").with(csrf())
						.session(adminSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"moduleCode\":\"safety\",\"title\":\"%s\"}".formatted(title)))
				.andExpect(status().isCreated())
				.andReturn();
		return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
	}

	private UUID generateAutoValidatedVersion(MockHttpSession adminSession, UUID lessonId) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/admin/lessons/" + lessonId + "/generate").with(csrf())
						.session(adminSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content(GENERATION_REQUEST_BODY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("auto_validated"))
				.andReturn();
		return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.versionId"));
	}

	/** Registers a fresh admin and returns a published lesson version's id. */
	private UUID createPublishedVersion(String title) throws Exception {
		String adminEmail = uniqueEmail("admin");
		registerAdmin(adminEmail);
		MockHttpSession adminSession = login(adminEmail);
		UUID lessonId = createLesson(adminSession, title);
		UUID versionId = generateAutoValidatedVersion(adminSession, lessonId);
		mockMvc.perform(post("/api/admin/lesson-versions/" + versionId + "/approve").with(csrf())
						.session(adminSession))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/admin/lesson-versions/" + versionId + "/publish").with(csrf())
						.session(adminSession))
				.andExpect(status().isOk());
		return versionId;
	}

	private String createGroupWithJoinCode(MockHttpSession teacherSession, UUID[] groupIdOut) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/groups").with(csrf())
						.session(teacherSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Class A\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		String body = result.getResponse().getContentAsString();
		groupIdOut[0] = UUID.fromString(JsonPath.read(body, "$.id"));
		return JsonPath.read(body, "$.joinCode");
	}

	private static String childBody(String displayName) {
		return "{\"displayName\":\"%s\",\"birthYear\":%d,\"relation\":\"mother\"}"
				.formatted(displayName, Year.now().getValue() - 3);
	}

	private UUID createActiveChildInGroup(MockHttpSession parentSession, String joinCode) throws Exception {
		MvcResult createResult = mockMvc.perform(post("/api/children").with(csrf())
						.session(parentSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content(childBody("Kid")))
				.andExpect(status().isCreated())
				.andReturn();
		String childId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");
		mockMvc.perform(post("/api/children/" + childId + "/consent").with(csrf()).session(parentSession))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/groups/join").with(csrf())
						.session(parentSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"joinCode\":\"%s\",\"childId\":\"%s\"}".formatted(joinCode, childId)))
				.andExpect(status().isOk());
		return UUID.fromString(childId);
	}

	/** Published lesson, assigned to a fresh group, with a verified parent's active child in that group. */
	private Fixture buildAssignedFixture() throws Exception {
		UUID versionId = createPublishedVersion("Crossing the street");

		String teacherEmail = uniqueEmail("teacher");
		registerTeacher(teacherEmail);
		MockHttpSession teacherSession = login(teacherEmail);
		UUID[] groupIdHolder = new UUID[1];
		String joinCode = createGroupWithJoinCode(teacherSession, groupIdHolder);
		UUID groupId = groupIdHolder[0];

		mockMvc.perform(post("/api/assignments").with(csrf())
						.session(teacherSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"lessonVersionId\":\"%s\",\"groupId\":\"%s\"}".formatted(versionId, groupId)))
				.andExpect(status().isCreated());

		String parentEmail = uniqueEmail("parent");
		registerVerifiedParent(parentEmail);
		MockHttpSession parentSession = login(parentEmail);
		UUID childId = createActiveChildInGroup(parentSession, joinCode);

		return new Fixture(versionId, groupId, joinCode, parentSession, childId);
	}

	@Test
	void parentGetsScenarioForAssignedPublishedLesson() throws Exception {
		Fixture fixture = buildAssignedFixture();

		mockMvc.perform(get("/api/children/" + fixture.childId() + "/lessons/" + fixture.versionId() + "/scenario")
						.session(fixture.parentSession()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.schema_version").value("1.0.0"))
				.andExpect(jsonPath("$.title").value("Crossing the street"))
				.andExpect(jsonPath("$.scenes").isArray());
	}

	@Test
	void unownedChildReturns404() throws Exception {
		Fixture fixture = buildAssignedFixture();

		String otherParentEmail = uniqueEmail("other");
		registerVerifiedParent(otherParentEmail);
		MockHttpSession otherSession = login(otherParentEmail);

		mockMvc.perform(get("/api/children/" + fixture.childId() + "/lessons/" + fixture.versionId() + "/scenario")
						.session(otherSession))
				.andExpect(status().isNotFound());
	}

	@Test
	void unassignedVersionReturns404() throws Exception {
		Fixture fixture = buildAssignedFixture();
		UUID unassignedVersionId = createPublishedVersion("Unassigned Lesson");

		mockMvc.perform(get("/api/children/" + fixture.childId() + "/lessons/" + unassignedVersionId + "/scenario")
						.session(fixture.parentSession()))
				.andExpect(status().isNotFound());
	}

	@Test
	void unpublishedVersionReturns404EvenIfDirectlyAssigned() throws Exception {
		Fixture fixture = buildAssignedFixture();

		String adminEmail = uniqueEmail("admin");
		User admin = registerAdmin(adminEmail);
		MockHttpSession adminSession = login(adminEmail);
		UUID lessonId = createLesson(adminSession, "Never Published");
		UUID unpublishedVersionId = generateAutoValidatedVersion(adminSession, lessonId);
		// Bypass the assignment endpoint (which itself refuses non-published targets) to prove
		// the scenario endpoint enforces published status on its own, independently.
		lessonAssignmentRepository
				.save(new LessonAssignment(unpublishedVersionId, null, fixture.childId(), admin.getId(), null, null));

		mockMvc.perform(get("/api/children/" + fixture.childId() + "/lessons/" + unpublishedVersionId + "/scenario")
						.session(fixture.parentSession()))
				.andExpect(status().isNotFound());
	}

	@Test
	void nonParentReturns403() throws Exception {
		Fixture fixture = buildAssignedFixture();

		String teacherEmail = uniqueEmail("teacher2");
		registerTeacher(teacherEmail);
		MockHttpSession teacherSession = login(teacherEmail);

		mockMvc.perform(get("/api/children/" + fixture.childId() + "/lessons/" + fixture.versionId() + "/scenario")
						.session(teacherSession))
				.andExpect(status().isForbidden());
	}

}
