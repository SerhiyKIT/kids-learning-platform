package ua.kidlearn.bootstrap;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * app.bootstrap.admin-token is blank by default (see application.yml / application-dev.yml —
 * neither profile sets ADMIN_BOOTSTRAP_TOKEN), so this class deliberately does NOT override it:
 * the endpoint must behave as if it doesn't exist, regardless of request body.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class BootstrapControllerTokenNotConfiguredTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void returnsNotFoundRegardlessOfBody() throws Exception {
		String body = "{\"token\":\"anything\",\"email\":\"%s\",\"password\":\"supersecret1\",\"displayName\":\"X\"}"
				.formatted(UUID.randomUUID() + "@example.test");

		mockMvc.perform(post("/api/bootstrap/admin").with(csrf())
						.header("X-Forwarded-For", "ip-" + UUID.randomUUID())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isNotFound());
	}

}
