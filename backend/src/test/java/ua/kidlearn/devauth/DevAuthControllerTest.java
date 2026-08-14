package ua.kidlearn.devauth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class DevAuthControllerTest {

	private static final String PASSWORD = "supersecret1";

	@Autowired
	private MockMvc mockMvc;

	private static String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@example.test";
	}

	private static String registerBody(String email, String displayName, String role) {
		return "{\"email\":\"%s\",\"password\":\"%s\",\"displayName\":\"%s\",\"role\":\"%s\"}"
				.formatted(email, PASSWORD, displayName, role);
	}

	@Test
	void registeredTeacherCanLogInAndMeReturnsTeacherRole() throws Exception {
		String email = uniqueEmail("dev-teacher");

		mockMvc.perform(post("/api/dev/register-role").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(registerBody(email, "Dev Teacher", "TEACHER")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.role").value("TEACHER"));

		MvcResult loginResult = mockMvc.perform(formLogin().user(email).password(PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("TEACHER"))
				.andExpect(jsonPath("$.emailVerified").value(true));
	}

	@Test
	void registeredAdminCanLogInAndMeReturnsAdminRole() throws Exception {
		String email = uniqueEmail("dev-admin");

		mockMvc.perform(post("/api/dev/register-role").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(registerBody(email, "Dev Admin", "ADMIN")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.role").value("ADMIN"));

		MvcResult loginResult = mockMvc.perform(formLogin().user(email).password(PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("ADMIN"))
				.andExpect(jsonPath("$.emailVerified").value(true));
	}

	@Test
	void duplicateEmailReturnsConflict() throws Exception {
		String email = uniqueEmail("dev-dup");
		mockMvc.perform(post("/api/dev/register-role").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(registerBody(email, "First", "TEACHER")))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/dev/register-role").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(registerBody(email, "Second", "ADMIN")))
				.andExpect(status().isConflict());
	}

}
