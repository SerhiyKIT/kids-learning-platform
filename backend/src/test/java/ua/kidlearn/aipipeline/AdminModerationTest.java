package ua.kidlearn.aipipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import ua.kidlearn.lessons.Lesson;
import ua.kidlearn.lessons.LessonRepository;
import ua.kidlearn.lessons.LessonVersion;
import ua.kidlearn.lessons.LessonVersionRepository;
import ua.kidlearn.scenario.ScenarioFixtures;
import ua.kidlearn.users.Role;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

/**
 * The moderation lifecycle that replaces the old unreviewed status-flip publish:
 * auto_validated -&gt; approve -&gt; approved -&gt; publish -&gt; published, or
 * auto_validated -&gt; reject -&gt; archived. Only 'approved' versions may publish; only
 * 'auto_validated' versions may be approved or rejected (docs/Формат_подання_уроків_та_ШІ.md §7).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class AdminModerationTest {

	private static final String PASSWORD = "supersecret1";

	private static final String GENERATION_REQUEST_BODY = """
			{"topic":"road_safety","patternIds":["1.6"],"ageBand":"age_5_6",
			 "title":"Crossing the street","learningGoal":"Look both ways before crossing"}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private StubLlmProvider stubLlmProvider;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private LessonVersionRepository lessonVersionRepository;

	@AfterEach
	void resetStub() {
		stubLlmProvider.clearForcedScenario();
	}

	private static String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@example.test";
	}

	private void registerTeacher(String email) {
		userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), Role.TEACHER, "Teacher", "uk"));
	}

	private MockHttpSession login(String email) throws Exception {
		MvcResult result = mockMvc.perform(formLogin().user(email).password(PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private User registerAndLoginAdmin(MockHttpSession[] sessionOut) throws Exception {
		String email = uniqueEmail("admin");
		User admin = userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), Role.ADMIN, "Admin", "uk"));
		sessionOut[0] = login(email);
		return admin;
	}

	private String createLesson(MockHttpSession adminSession, String title) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/admin/lessons").with(csrf())
						.session(adminSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"moduleCode\":\"safety\",\"title\":\"%s\"}".formatted(title)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	/** Generates a version for a fresh lesson and returns its id; asserts the given expected status. */
	private String generateVersion(MockHttpSession adminSession, String expectedStatus) throws Exception {
		String lessonId = createLesson(adminSession, "Crossing the street");
		MvcResult result = mockMvc.perform(post("/api/admin/lessons/" + lessonId + "/generate").with(csrf())
						.session(adminSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content(GENERATION_REQUEST_BODY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(expectedStatus))
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.versionId");
	}

	@Test
	void happyPathGenerateApprovePublishAppearsInCatalog() throws Exception {
		MockHttpSession[] sessionHolder = new MockHttpSession[1];
		User admin = registerAndLoginAdmin(sessionHolder);
		MockHttpSession adminSession = sessionHolder[0];
		String versionId = generateVersion(adminSession, "auto_validated");

		mockMvc.perform(post("/api/admin/lesson-versions/" + versionId + "/approve").with(csrf())
						.session(adminSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("approved"));

		LessonVersion approved = lessonVersionRepository.findById(UUID.fromString(versionId)).orElseThrow();
		assertThat(approved.getStatus()).isEqualTo(LessonVersion.STATUS_APPROVED);
		assertThat(approved.getApprovedBy()).isEqualTo(admin.getId());

		mockMvc.perform(post("/api/admin/lesson-versions/" + versionId + "/publish").with(csrf())
						.session(adminSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("published"));

		LessonVersion published = lessonVersionRepository.findById(UUID.fromString(versionId)).orElseThrow();
		Lesson lesson = lessonRepository.findById(published.getLessonId()).orElseThrow();
		assertThat(lesson.getCurrentVersionId()).isEqualTo(published.getId());

		mockMvc.perform(get("/api/catalog/lessons").session(adminSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.currentVersionId=='" + versionId + "')]").isNotEmpty());
	}

	@Test
	void approvingRejectedAutoVersionReturns409NotValidated() throws Exception {
		MockHttpSession[] sessionHolder = new MockHttpSession[1];
		registerAndLoginAdmin(sessionHolder);
		MockHttpSession adminSession = sessionHolder[0];

		JsonNode invalidScenario = objectMapper
				.readTree(ScenarioFixtures.VALID_MINIMAL_SCENARIO.replace("\"title\": \"Crossing the street\",", ""));
		stubLlmProvider.forceNextScenario(invalidScenario);
		String versionId = generateVersion(adminSession, "rejected_auto");

		mockMvc.perform(post("/api/admin/lesson-versions/" + versionId + "/approve").with(csrf())
						.session(adminSession))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("NOT_VALIDATED"));
	}

	@Test
	void publishingUnapprovedAutoValidatedVersionReturns409NotApproved() throws Exception {
		MockHttpSession[] sessionHolder = new MockHttpSession[1];
		registerAndLoginAdmin(sessionHolder);
		MockHttpSession adminSession = sessionHolder[0];
		String versionId = generateVersion(adminSession, "auto_validated");

		mockMvc.perform(post("/api/admin/lesson-versions/" + versionId + "/publish").with(csrf())
						.session(adminSession))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("NOT_APPROVED"));
	}

	@Test
	void rejectingAutoValidatedVersionArchivesItAndKeepsItOutOfCatalog() throws Exception {
		MockHttpSession[] sessionHolder = new MockHttpSession[1];
		registerAndLoginAdmin(sessionHolder);
		MockHttpSession adminSession = sessionHolder[0];
		String versionId = generateVersion(adminSession, "auto_validated");

		mockMvc.perform(post("/api/admin/lesson-versions/" + versionId + "/reject").with(csrf())
						.session(adminSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"reason\":\"Inaccurate facts\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("archived"));

		LessonVersion rejected = lessonVersionRepository.findById(UUID.fromString(versionId)).orElseThrow();
		assertThat(rejected.getStatus()).isEqualTo(LessonVersion.STATUS_ARCHIVED);
		Lesson lesson = lessonRepository.findById(rejected.getLessonId()).orElseThrow();
		assertThat(lesson.getCurrentVersionId()).isNull();

		mockMvc.perform(get("/api/catalog/lessons").session(adminSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.lessonId=='" + rejected.getLessonId() + "')]").isEmpty());
	}

	@Test
	void pendingReviewListReturnsOnlyAutoValidatedVersions() throws Exception {
		MockHttpSession[] sessionHolder = new MockHttpSession[1];
		registerAndLoginAdmin(sessionHolder);
		MockHttpSession adminSession = sessionHolder[0];

		String stillPendingVersionId = generateVersion(adminSession, "auto_validated");
		String approvedVersionId = generateVersion(adminSession, "auto_validated");
		mockMvc.perform(post("/api/admin/lesson-versions/" + approvedVersionId + "/approve").with(csrf())
						.session(adminSession))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/admin/lesson-versions").session(adminSession).param("status", "auto_validated"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.versionId=='" + stillPendingVersionId + "')]").isNotEmpty())
				.andExpect(jsonPath("$[?(@.versionId=='" + approvedVersionId + "')]").isEmpty())
				.andExpect(jsonPath("$[?(@.versionId=='" + stillPendingVersionId + "')].status")
						.value("auto_validated"));
	}

	@Test
	void nonAdminCannotModerate() throws Exception {
		MockHttpSession[] sessionHolder = new MockHttpSession[1];
		registerAndLoginAdmin(sessionHolder);
		MockHttpSession adminSession = sessionHolder[0];
		String versionId = generateVersion(adminSession, "auto_validated");

		String teacherEmail = uniqueEmail("teacher");
		registerTeacher(teacherEmail);
		MockHttpSession teacherSession = login(teacherEmail);

		mockMvc.perform(get("/api/admin/lesson-versions").session(teacherSession).param("status", "auto_validated"))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/admin/lesson-versions/" + versionId).session(teacherSession))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/admin/lesson-versions/" + versionId + "/approve").with(csrf())
						.session(teacherSession))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/admin/lesson-versions/" + versionId + "/reject").with(csrf())
						.session(teacherSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"reason\":\"nope\"}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/admin/lesson-versions/" + versionId + "/publish").with(csrf())
						.session(teacherSession))
				.andExpect(status().isForbidden());
	}

}
