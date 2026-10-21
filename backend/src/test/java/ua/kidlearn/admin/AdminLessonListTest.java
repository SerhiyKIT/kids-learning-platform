package ua.kidlearn.admin;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import ua.kidlearn.lessons.LessonService;
import ua.kidlearn.lessons.LessonVersion;
import ua.kidlearn.scenario.ScenarioFixtures;
import ua.kidlearn.support.WebIntegrationTest;
import ua.kidlearn.twofa.AdminTwoFactorTestSupport;
import ua.kidlearn.users.Role;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

/**
 * GET /api/admin/lessons: every lesson with its version summary, ADMIN only. (Newest-first order
 * isn't asserted here: inside one test transaction every row gets the same created_at = now().)
 */
@Transactional
class AdminLessonListTest extends WebIntegrationTest {

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

	private MockHttpSession login(Role role) throws Exception {
		String email = role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.test";
		userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), role, "User", "uk"));
		MvcResult result = mockMvc.perform(formLogin().user(email).password(PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
		if (role == Role.ADMIN) {
			AdminTwoFactorTestSupport.completeSetup(mockMvc, session);
		}
		return session;
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

	@Test
	void adminSeesEveryLessonNewestFirstWithItsLatestVersionAndPublishedVersion() throws Exception {
		MockHttpSession admin = login(Role.ADMIN);
		String emptyTitle = "Empty " + UUID.randomUUID();
		String publishedTitle = "Published " + UUID.randomUUID();
		UUID emptyLesson = createLesson(admin, emptyTitle);
		UUID publishedLesson = createLesson(admin, publishedTitle);

		UUID v1 = lessonService.createGeneratedVersion(publishedLesson,
				objectMapper.readTree(ScenarioFixtures.VALID_MINIMAL_SCENARIO), "test", LessonVersion.STATUS_AUTO_VALIDATED)
				.getId();
		mockMvc.perform(post("/api/admin/lesson-versions/" + v1 + "/approve").with(csrf()).session(admin))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/admin/lesson-versions/" + v1 + "/publish").with(csrf()).session(admin))
				.andExpect(status().isOk());
		UUID v2 = lessonService.createGeneratedVersion(publishedLesson,
				objectMapper.readTree(ScenarioFixtures.VALID_MINIMAL_SCENARIO), "test", LessonVersion.STATUS_REJECTED_AUTO)
				.getId();

		String published = "$[?(@.id == '" + publishedLesson + "')]";
		String empty = "$[?(@.id == '" + emptyLesson + "')]";
		mockMvc.perform(get("/api/admin/lessons").session(admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath(published + ".title").value(publishedTitle))
				.andExpect(jsonPath(published + ".moduleCode").value("safety"))
				.andExpect(jsonPath(published + ".versionCount").value(2))
				.andExpect(jsonPath(published + ".latestVersion.id").value(v2.toString()))
				.andExpect(jsonPath(published + ".latestVersion.versionNo").value(2))
				.andExpect(jsonPath(published + ".latestVersion.status").value("rejected_auto"))
				.andExpect(jsonPath(published + ".publishedVersionId").value(v1.toString()))
				.andExpect(jsonPath(empty + ".versionCount").value(0))
				.andExpect(jsonPath(empty + ".latestVersion").value(Matchers.contains(Matchers.nullValue())))
				.andExpect(jsonPath(empty + ".publishedVersionId").value(Matchers.contains(Matchers.nullValue())));
	}

	@Test
	void parentAndTeacherAreForbidden() throws Exception {
		mockMvc.perform(get("/api/admin/lessons").session(login(Role.PARENT))).andExpect(status().isForbidden());
		mockMvc.perform(get("/api/admin/lessons").session(login(Role.TEACHER))).andExpect(status().isForbidden());
	}

	@Test
	void anonymousIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/admin/lessons")).andExpect(status().isUnauthorized());
	}

}
