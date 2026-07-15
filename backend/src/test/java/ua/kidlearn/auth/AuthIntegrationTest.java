package ua.kidlearn.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs against the real dev PostgreSQL (docker compose -f infra/docker-compose.yml
 * up -d postgres must be running); each test rolls back via @Transactional.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class AuthIntegrationTest {

	private static final String EMAIL = "auth-test-parent@example.test";
	private static final String PASSWORD = "supersecret1";
	private static final String REGISTER_BODY = """
			{"email":"%s","password":"%s","displayName":"Test Parent"}
			""".formatted(EMAIL, PASSWORD);

	@Autowired
	private MockMvc mockMvc;

	private MockHttpSession registerAndLogin() throws Exception {
		mockMvc.perform(post("/api/auth/register").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(REGISTER_BODY))
				.andExpect(status().isCreated());

		MvcResult loginResult = mockMvc.perform(formLogin().user(EMAIL).password(PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		return (MockHttpSession) loginResult.getRequest().getSession(false);
	}

	@Test
	void registerSucceedsThenRejectsDuplicateEmail() throws Exception {
		mockMvc.perform(post("/api/auth/register").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(REGISTER_BODY))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(EMAIL))
				.andExpect(jsonPath("$.displayName").value("Test Parent"));

		mockMvc.perform(post("/api/auth/register").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(REGISTER_BODY))
				.andExpect(status().isConflict());
	}

	@Test
	void unauthenticatedMeIsRejected() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void loginThenMeReturnsTheUser() throws Exception {
		MockHttpSession session = registerAndLogin();

		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(EMAIL))
				.andExpect(jsonPath("$.role").value("PARENT"));
	}

	@Test
	void parentIsDeniedAdminEndpoint() throws Exception {
		MockHttpSession session = registerAndLogin();

		mockMvc.perform(get("/api/admin/ping").session(session))
				.andExpect(status().isForbidden());
	}

}
