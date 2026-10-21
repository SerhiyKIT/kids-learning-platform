package ua.kidlearn.aipipeline;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import ua.kidlearn.lessons.LessonService;
import ua.kidlearn.lessons.LessonVersion;
import ua.kidlearn.scenario.ScenarioFixtures;
import ua.kidlearn.twofa.AdminTwoFactorTestSupport;
import ua.kidlearn.users.Role;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

/**
 * Content-pipeline guard rails: a malformed generation request is rejected with per-field
 * problems before any LLM call, and only moderated (approved/published) versions can be voiced.
 * In-memory audio storage, so no MinIO is needed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = "app.tts.storage=memory")
@Import(InMemoryAudioStorageTestConfig.class)
@Transactional
class ContentPipelineRulesTest {

	private static final String PASSWORD = "supersecret1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private LessonService lessonService;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoSpyBean
	private LlmProvider llmProvider;

	private MockHttpSession loginAsNewAdmin() throws Exception {
		String email = "admin-" + UUID.randomUUID() + "@example.test";
		userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), Role.ADMIN, "Admin", "uk"));
		MvcResult result = mockMvc.perform(formLogin().user(email).password(PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
		AdminTwoFactorTestSupport.completeSetup(mockMvc, session);
		return session;
	}

	private UUID createLesson(MockHttpSession adminSession) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/admin/lessons").with(csrf())
						.session(adminSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"moduleCode\":\"safety\",\"title\":\"Crossing the street\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
	}

	private UUID createVersion(UUID lessonId, String status) {
		return lessonService.createGeneratedVersion(lessonId,
				objectMapper.readTree(ScenarioFixtures.VALID_MINIMAL_SCENARIO), "test-fixture", status).getId();
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{\"topic\":\"fire_safety\",\"patternIds\":[\"1.6\"],\"ageBand\":\"age_5_6\"}",
			"{\"topic\":\"road_safety\",\"patternIds\":[\"1.6\"],\"ageBand\":\"age_7_8\"}",
			"{\"topic\":\"road_safety\",\"patternIds\":[],\"ageBand\":\"age_5_6\"}",
			"{\"topic\":\"road_safety\",\"patternIds\":[\"1.1\",\"1.2\",\"1.3\"],\"ageBand\":\"age_5_6\"}",
			"{\"topic\":\"road_safety\",\"patternIds\":[\"3.1\"],\"ageBand\":\"age_5_6\"}",
			"{\"topic\":\"road_safety\",\"patternIds\":[\"1.0\"],\"ageBand\":\"age_5_6\"}",
			"{}" })
	void invalidGenerationRequestIs400WithFieldProblemsAndNeverCallsTheLlm(String body) throws Exception {
		MockHttpSession admin = loginAsNewAdmin();
		UUID lessonId = createLesson(admin);

		mockMvc.perform(post("/api/admin/lessons/" + lessonId + "/generate").with(csrf())
						.session(admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_GENERATION_REQUEST"))
				.andExpect(jsonPath("$.problems").isNotEmpty())
				.andExpect(jsonPath("$.problems[*].path",
						Matchers.everyItem(Matchers.anyOf(Matchers.is("topic"), Matchers.is("ageBand"),
								Matchers.startsWith("patternIds")))));

		verify(llmProvider, never()).generate(any());
	}

	@Test
	void everyBrokenFieldIsReportedAtOnce() throws Exception {
		MockHttpSession admin = loginAsNewAdmin();
		UUID lessonId = createLesson(admin);

		mockMvc.perform(post("/api/admin/lessons/" + lessonId + "/generate").with(csrf())
						.session(admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"topic\":\"nope\",\"patternIds\":[\"1.6\",\"x\"],\"ageBand\":\"nope\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.problems[*].path", Matchers.containsInAnyOrder("topic", "ageBand", "patternIds/1")));

		verify(llmProvider, never()).generate(any());
	}

	@Test
	void validGenerationRequestStillReachesTheLlm() throws Exception {
		MockHttpSession admin = loginAsNewAdmin();
		UUID lessonId = createLesson(admin);

		mockMvc.perform(post("/api/admin/lessons/" + lessonId + "/generate").with(csrf())
						.session(admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"topic\":\"emergency_numbers\",\"patternIds\":[\"2.1\",\"2.3\"],\"ageBand\":\"age_3_4\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("auto_validated"));

		verify(llmProvider).generate(any());
	}

	@ParameterizedTest
	@ValueSource(strings = { LessonVersion.STATUS_AUTO_VALIDATED, LessonVersion.STATUS_REJECTED_AUTO })
	void voicingUnmoderatedVersionIs409(String status) throws Exception {
		MockHttpSession admin = loginAsNewAdmin();
		UUID versionId = createVersion(createLesson(admin), status);

		mockMvc.perform(post("/api/admin/lesson-versions/" + versionId + "/voice").with(csrf()).session(admin))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("VOICING_REQUIRES_APPROVED"));
	}

	@Test
	void voicingRejectedByModeratorIs409() throws Exception {
		MockHttpSession admin = loginAsNewAdmin();
		UUID versionId = createVersion(createLesson(admin), LessonVersion.STATUS_AUTO_VALIDATED);
		mockMvc.perform(post("/api/admin/lesson-versions/" + versionId + "/reject").with(csrf())
						.session(admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"reason\":\"not suitable\"}"))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/admin/lesson-versions/" + versionId + "/voice").with(csrf()).session(admin))
				.andExpect(status().isConflict());
	}

	@Test
	void voicingApprovedAndPublishedVersionsSucceeds() throws Exception {
		MockHttpSession admin = loginAsNewAdmin();
		UUID versionId = createVersion(createLesson(admin), LessonVersion.STATUS_AUTO_VALIDATED);
		mockMvc.perform(post("/api/admin/lesson-versions/" + versionId + "/approve").with(csrf()).session(admin))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/admin/lesson-versions/" + versionId + "/voice").with(csrf()).session(admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.linesTotal").isNumber());

		mockMvc.perform(post("/api/admin/lesson-versions/" + versionId + "/publish").with(csrf()).session(admin))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/admin/lesson-versions/" + versionId + "/voice").with(csrf()).session(admin))
				.andExpect(status().isOk());
	}

}
