package ua.kidlearn.aipipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
import ua.kidlearn.config.LlmProperties;
import ua.kidlearn.lessons.Lesson;
import ua.kidlearn.lessons.LessonRepository;
import ua.kidlearn.lessons.LessonVersion;
import ua.kidlearn.lessons.LessonVersionRepository;
import ua.kidlearn.scenario.ScenarioFixtures;
import ua.kidlearn.users.Role;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

/**
 * End-to-end: POST /api/admin/lessons/{id}/generate runs the generate -> validate -> bounded
 * retry -> persist pipeline (docs/Промпт_генерації_уроків.md §3), never auto-publishing.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class AdminGenerationControllerTest {

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

	@Autowired
	private LlmProperties llmProperties;

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

	private MockHttpSession loginAsNewAdmin() throws Exception {
		String email = uniqueEmail("admin");
		userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), Role.ADMIN, "Admin", "uk"));
		MvcResult result = mockMvc.perform(formLogin().user(email).password(PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private MockHttpSession login(String email) throws Exception {
		MvcResult result = mockMvc.perform(formLogin().user(email).password(PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private String createLesson(MockHttpSession adminSession) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/admin/lessons").with(csrf())
						.session(adminSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"moduleCode\":\"safety\",\"title\":\"Crossing the street\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	@Test
	void validStubScenarioIsPersistedAsAutoValidated() throws Exception {
		MockHttpSession adminSession = loginAsNewAdmin();
		String lessonId = createLesson(adminSession);

		MvcResult result = mockMvc.perform(post("/api/admin/lessons/" + lessonId + "/generate").with(csrf())
						.session(adminSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content(GENERATION_REQUEST_BODY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("auto_validated"))
				.andExpect(jsonPath("$.problems").isEmpty())
				.andReturn();
		String versionId = JsonPath.read(result.getResponse().getContentAsString(), "$.versionId");

		LessonVersion version = lessonVersionRepository.findById(UUID.fromString(versionId)).orElseThrow();
		assertThat(version.getStatus()).isEqualTo(LessonVersion.STATUS_AUTO_VALIDATED);
		assertThat(version.getGeneratedBy()).isEqualTo("ai");
		assertThat(version.getAiModel()).isEqualTo("stub");
	}

	@Test
	void forcedInvalidScenarioIsPersistedAsRejectedAutoAfterMaxAttemptsAndNeverPublished() throws Exception {
		MockHttpSession adminSession = loginAsNewAdmin();
		String lessonId = createLesson(adminSession);

		JsonNode invalidScenario = objectMapper
				.readTree(ScenarioFixtures.VALID_MINIMAL_SCENARIO.replace("\"title\": \"Crossing the street\",", ""));
		stubLlmProvider.forceNextScenario(invalidScenario);

		MvcResult result = mockMvc.perform(post("/api/admin/lessons/" + lessonId + "/generate").with(csrf())
						.session(adminSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content(GENERATION_REQUEST_BODY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("rejected_auto"))
				.andExpect(jsonPath("$.problems").isNotEmpty())
				.andExpect(jsonPath("$.attempts").value(llmProperties.maxAttempts()))
				.andReturn();
		String versionId = JsonPath.read(result.getResponse().getContentAsString(), "$.versionId");

		LessonVersion version = lessonVersionRepository.findById(UUID.fromString(versionId)).orElseThrow();
		assertThat(version.getStatus()).isEqualTo(LessonVersion.STATUS_REJECTED_AUTO);

		Lesson lesson = lessonRepository.findById(UUID.fromString(lessonId)).orElseThrow();
		assertThat(lesson.getCurrentVersionId()).isNull();
	}

	@Test
	void nonAdminCannotGenerate() throws Exception {
		MockHttpSession adminSession = loginAsNewAdmin();
		String lessonId = createLesson(adminSession);

		String teacherEmail = uniqueEmail("teacher");
		registerTeacher(teacherEmail);
		MockHttpSession teacherSession = login(teacherEmail);

		mockMvc.perform(post("/api/admin/lessons/" + lessonId + "/generate").with(csrf())
						.session(teacherSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content(GENERATION_REQUEST_BODY))
				.andExpect(status().isForbidden());
	}

}
