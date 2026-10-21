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
 * app.bootstrap.admin-token is blank by default (no profile sets ADMIN_BOOTSTRAP_TOKEN), so the
 * endpoint must behave as if it doesn't exist, regardless of request body. Runs in the prod-like
 * "guardtest" context (shared with DevRoutesAbsentOutsideDevProfileTest): the dev-profile tests
 * share a context that configures a token (support.WebIntegrationTest).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("guardtest")
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
