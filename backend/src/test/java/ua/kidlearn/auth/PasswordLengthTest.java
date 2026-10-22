package ua.kidlearn.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import ua.kidlearn.support.WebIntegrationTest;

/**
 * QA1-008: BCrypt takes at most 72 bytes. A longer new password (a Cyrillic passphrase of 37+
 * letters) used to reach the encoder and come back as a 500; it is now a validation error on
 * every endpoint that sets a password, while a password of exactly 72 bytes still works.
 */
@Transactional
class PasswordLengthTest extends WebIntegrationTest {

	/** 36 Cyrillic letters = 72 bytes of UTF-8: the longest password that still fits. */
	private static final String LONGEST = "пароль".repeat(6);

	private static final String TOO_LONG = LONGEST + "ь";

	@Autowired
	private MockMvc mockMvc;

	private ResultActions postJson(String path, String json, MockHttpSession session) throws Exception {
		var request = post(path).with(csrf())
				.header("X-Forwarded-For", "ip-" + UUID.randomUUID())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json);
		return mockMvc.perform(session == null ? request : request.session(session));
	}

	private String register(String password) throws Exception {
		String email = "long-password-" + UUID.randomUUID() + "@example.test";
		postJson("/api/auth/register",
				"{\"email\":\"%s\",\"password\":\"%s\",\"displayName\":\"Тест\"}".formatted(email, password), null)
				.andExpect(status().isCreated());
		return email;
	}

	@Test
	void registrationRejectsAPasswordOver72BytesAndAcceptsExactly72() throws Exception {
		postJson("/api/auth/register", "{\"email\":\"long-%s@example.test\",\"password\":\"%s\",\"displayName\":\"Тест\"}"
				.formatted(UUID.randomUUID(), TOO_LONG), null)
				.andExpect(status().isBadRequest());

		String email = register(LONGEST);
		mockMvc.perform(formLogin().user(email).password(LONGEST)).andExpect(redirectedUrl("/"));
	}

	@Test
	void resetInviteAndPasswordChangeRejectAPasswordOver72Bytes() throws Exception {
		postJson("/api/auth/reset-password", "{\"token\":\"t\",\"newPassword\":\"%s\"}".formatted(TOO_LONG), null)
				.andExpect(status().isBadRequest());
		postJson("/api/auth/accept-invite", "{\"token\":\"t\",\"password\":\"%s\"}".formatted(TOO_LONG), null)
				.andExpect(status().isBadRequest());

		String email = register("supersecret1");
		MvcResult login = mockMvc.perform(formLogin().user(email).password("supersecret1")).andReturn();
		MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
		postJson("/api/me/password", "{\"currentPassword\":\"supersecret1\",\"newPassword\":\"%s\"}".formatted(TOO_LONG), session)
				.andExpect(status().isBadRequest());
		postJson("/api/me/password", "{\"currentPassword\":\"supersecret1\",\"newPassword\":\"%s\"}".formatted(LONGEST), session)
				.andExpect(status().isNoContent());
	}

}
