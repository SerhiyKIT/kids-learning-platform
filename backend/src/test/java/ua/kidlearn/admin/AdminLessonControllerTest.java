package ua.kidlearn.admin;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.transaction.annotation.Transactional;
import ua.kidlearn.users.Role;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class AdminLessonControllerTest {

	private static final String PASSWORD = "supersecret1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private static String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@example.test";
	}

	private void registerAdmin(String email) {
		userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), Role.ADMIN, "Admin", "uk"));
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

	@Test
	void adminCreatesLessonPublishesAndCatalogExcludesTheDraft() throws Exception {
		String adminEmail = uniqueEmail("admin");
		registerAdmin(adminEmail);
		MockHttpSession adminSession = login(adminEmail);

		MvcResult lessonResult = mockMvc.perform(post("/api/admin/lessons").with(csrf())
						.session(adminSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"moduleCode\":\"safety\",\"title\":\"Crossing the street\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		String lessonId = JsonPath.read(lessonResult.getResponse().getContentAsString(), "$.id");

		MvcResult versionResult = mockMvc.perform(post("/api/admin/lessons/" + lessonId + "/versions").with(csrf())
						.session(adminSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"scenario\":{\"learning_goal\":\"Cross safely\",\"scenes\":[{\"type\":\"choice_situation\"}]},"
								+ "\"generatedBy\":\"human\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("draft"))
				.andExpect(jsonPath("$.versionNo").value(1))
				.andReturn();
		String versionId = JsonPath.read(versionResult.getResponse().getContentAsString(), "$.id");

		// Draft version: lesson has no current_version_id yet, so it's absent from the catalog.
		mockMvc.perform(get("/api/catalog/lessons").session(adminSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.lessonId=='" + lessonId + "')]").isEmpty());

		mockMvc.perform(post("/api/admin/lesson-versions/" + versionId + "/publish").with(csrf())
						.session(adminSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("published"));

		mockMvc.perform(get("/api/catalog/lessons").session(adminSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.lessonId=='" + lessonId + "')].title").value("Crossing the street"))
				.andExpect(jsonPath("$[?(@.lessonId=='" + lessonId + "')].moduleCode").value("safety"))
				.andExpect(jsonPath("$[?(@.lessonId=='" + lessonId + "')].currentVersionId").value(versionId));
	}

	@Test
	void nonAdminCannotCreateLesson() throws Exception {
		String teacherEmail = uniqueEmail("teacher");
		registerTeacher(teacherEmail);
		MockHttpSession teacherSession = login(teacherEmail);

		mockMvc.perform(post("/api/admin/lessons").with(csrf())
						.session(teacherSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"moduleCode\":\"safety\",\"title\":\"Sneaky\"}"))
				.andExpect(status().isForbidden());
	}

}
