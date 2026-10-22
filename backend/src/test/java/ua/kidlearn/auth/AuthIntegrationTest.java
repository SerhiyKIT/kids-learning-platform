package ua.kidlearn.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import ua.kidlearn.support.WebIntegrationTest;

/**
 * Runs against the real dev PostgreSQL (docker compose -f infra/docker-compose.yml
 * up -d postgres must be running); each test rolls back via @Transactional.
 */
@Transactional
class AuthIntegrationTest extends WebIntegrationTest {

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
				.andExpect(status().isAccepted());

		MvcResult loginResult = mockMvc.perform(formLogin().user(EMAIL).password(PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		return (MockHttpSession) loginResult.getRequest().getSession(false);
	}

	@Test
	void registerAnswersTheSameForATakenEmailAndKeepsOneAccount() throws Exception {
		String first = mockMvc.perform(post("/api/auth/register").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(REGISTER_BODY))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.status").value("check_email"))
				.andReturn().getResponse().getContentAsString();

		String again = mockMvc.perform(post("/api/auth/register").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(REGISTER_BODY))
				.andExpect(status().isAccepted())
				.andReturn().getResponse().getContentAsString();
		assertThat(again).isEqualTo(first);
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
