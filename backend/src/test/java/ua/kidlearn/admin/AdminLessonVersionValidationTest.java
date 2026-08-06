package ua.kidlearn.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import ua.kidlearn.lessons.LessonVersionRepository;
import ua.kidlearn.scenario.ScenarioFixtures;
import ua.kidlearn.users.Role;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

/**
 * End-to-end: POST /api/admin/lessons/{id}/versions validates the scenario (structural schema +
 * business rules) before persisting, per docs/lesson-scenario.schema.json and
 * docs/Формат_подання_уроків_та_ШІ.md §4.2.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class AdminLessonVersionValidationTest {

	private static final String PASSWORD = "supersecret1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private LessonVersionRepository lessonVersionRepository;

	private static String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@example.test";
	}

	private MockHttpSession loginAsNewAdmin() throws Exception {
		String email = uniqueEmail("admin");
		userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), Role.ADMIN, "Admin", "uk"));
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

	private ResultActions postVersion(MockHttpSession adminSession, String lessonId, String scenarioJson)
			throws Exception {
		return mockMvc.perform(post("/api/admin/lessons/" + lessonId + "/versions").with(csrf())
				.session(adminSession)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"scenario\":" + scenarioJson + ",\"generatedBy\":\"human\"}"));
	}

	@Test
	void validMinimalScenarioIsAccepted() throws Exception {
		MockHttpSession adminSession = loginAsNewAdmin();
		String lessonId = createLesson(adminSession);

		postVersion(adminSession, lessonId, ScenarioFixtures.VALID_MINIMAL_SCENARIO)
				.andExpect(status().isCreated());
	}

	@Test
	void missingRequiredFieldReturns422WithProblemsAndPersistsNothing() throws Exception {
		MockHttpSession adminSession = loginAsNewAdmin();
		String lessonId = createLesson(adminSession);
		String scenario = ScenarioFixtures.VALID_MINIMAL_SCENARIO.replace("\"title\": \"Crossing the street\",", "");

		postVersion(adminSession, lessonId, scenario)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.problems").isNotEmpty());

		assertThat(lessonVersionRepository.findFirstByLessonIdOrderByVersionNoDesc(UUID.fromString(lessonId)))
				.isEmpty();
	}

	@Test
	void wrongEnumValueReturns422WithProblems() throws Exception {
		MockHttpSession adminSession = loginAsNewAdmin();
		String lessonId = createLesson(adminSession);
		String scenario = ScenarioFixtures.VALID_MINIMAL_SCENARIO.replace("\"topic\": \"road_safety\"",
				"\"topic\": \"not_a_real_topic\"");

		postVersion(adminSession, lessonId, scenario)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.problems").isNotEmpty());
	}

	@Test
	void extraPropertyReturns422WithProblems() throws Exception {
		MockHttpSession adminSession = loginAsNewAdmin();
		String lessonId = createLesson(adminSession);
		String scenario = ScenarioFixtures.VALID_MINIMAL_SCENARIO.replaceFirst("\\{",
				"{\n  \"unexpected_field\": \"nope\",");

		postVersion(adminSession, lessonId, scenario)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.problems").isNotEmpty());
	}

	@Test
	void duplicateVoiceLineKeyReturns422WithProblems() throws Exception {
		MockHttpSession adminSession = loginAsNewAdmin();
		String lessonId = createLesson(adminSession);
		String scenario = ScenarioFixtures.VALID_MINIMAL_SCENARIO.replace("\"hint2\"", "\"hint1\"");

		postVersion(adminSession, lessonId, scenario)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.problems").isNotEmpty());
	}

	@Test
	void twoCorrectOptionsReturns422WithProblems() throws Exception {
		MockHttpSession adminSession = loginAsNewAdmin();
		String lessonId = createLesson(adminSession);
		String scenario = ScenarioFixtures.VALID_MINIMAL_SCENARIO.replace(
				"\"id\": \"b\", \"icon\": \"icon_run\", \"label\": {\"key\": \"opt_b\", \"text\": \"Run across fast\"}, \"correct\": false",
				"\"id\": \"b\", \"icon\": \"icon_run\", \"label\": {\"key\": \"opt_b\", \"text\": \"Run across fast\"}, \"correct\": true");

		postVersion(adminSession, lessonId, scenario)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.problems").isNotEmpty());
	}

	@Test
	void lastInteractiveSceneNotControlReturns422WithProblems() throws Exception {
		MockHttpSession adminSession = loginAsNewAdmin();
		String lessonId = createLesson(adminSession);
		String scenario = ScenarioFixtures.VALID_MINIMAL_SCENARIO.replace(
				"\"characters\": [{\"id\": \"guide\", \"emotion\": \"pride\"}],\n      \"is_control\": true,",
				"\"characters\": [{\"id\": \"guide\", \"emotion\": \"pride\"}],\n      \"is_control\": false,");

		postVersion(adminSession, lessonId, scenario)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.problems").isNotEmpty());
	}

	@Test
	void overLengthVoiceLineReturns422WithProblems() throws Exception {
		MockHttpSession adminSession = loginAsNewAdmin();
		String lessonId = createLesson(adminSession);
		String scenario = ScenarioFixtures.VALID_MINIMAL_SCENARIO.replace(
				"\"key\": \"narr1\", \"text\": \"Watch me cross the street safely\"",
				"\"key\": \"narr1\", \"text\": \"Watch me cross the busy street very safely every single time we go outside\"");

		postVersion(adminSession, lessonId, scenario)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.problems").isNotEmpty());
	}

}
