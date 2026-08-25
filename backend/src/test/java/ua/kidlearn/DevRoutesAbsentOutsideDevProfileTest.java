package ua.kidlearn;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import ua.kidlearn.devauth.DevAuthController;
import ua.kidlearn.devseed.DevSeedController;

/**
 * Full-context regression guard: proves the dev-only routes are genuinely unreachable under a
 * profile that is NOT "dev" — not just "the bean is missing in isolation" (see
 * DevAuthControllerProfileTest / DevSeedControllerProfileTest for that narrower, DB-free check)
 * but "a real request through the full security/web stack 404s, because the handler mapping
 * itself doesn't exist".
 *
 * Uses a dedicated "guardtest" profile (src/test/resources/application-guardtest.yml) rather than
 * "prod": application-prod.yml requires several env vars with no defaults (DATASOURCE_*, SMTP_*,
 * MINIO_*, TWOFA_ENC_KEY, ...), which would make this test depend on secrets that must not be
 * committed. "guardtest" supplies the same test-friendly values application-dev.yml does (same
 * Postgres/MinIO/Mailpit defaults CI and infra/docker-compose.yml already provide), minus the
 * "dev" profile itself — so @Profile("dev") beans are genuinely absent while everything else
 * still boots cleanly.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("guardtest")
@Transactional
class DevRoutesAbsentOutsideDevProfileTest {

	private static final String PASSWORD = "supersecret1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void devControllerBeansAreAbsentFromTheContext() {
		assertThatThrownBy(() -> applicationContext.getBean(DevAuthController.class))
				.isInstanceOf(NoSuchBeanDefinitionException.class);
		assertThatThrownBy(() -> applicationContext.getBean(DevSeedController.class))
				.isInstanceOf(NoSuchBeanDefinitionException.class);
	}

	@Test
	void registerRoleRouteIsUnmapped() throws Exception {
		// permitAll in SecurityConfig (must be reachable pre-session in "dev"), so an
		// unauthenticated call reaches the dispatcher directly and finds no handler.
		mockMvc.perform(post("/api/dev/register-role").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void seedDemoRouteIsUnmapped() throws Exception {
		// Not permitAll, so an unauthenticated call would 401 before ever reaching the
		// dispatcher — that would only prove "you need a session", not "the route is gone".
		// Use a real authenticated session so the request actually gets as far as routing.
		MockHttpSession session = registerAndLoginParent();

		mockMvc.perform(post("/api/dev/seed-demo").with(csrf()).session(session))
				.andExpect(status().isNotFound());
	}

	private MockHttpSession registerAndLoginParent() throws Exception {
		String email = "guardtest-parent-" + UUID.randomUUID() + "@example.test";
		mockMvc.perform(post("/api/auth/register").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"%s\",\"password\":\"%s\",\"displayName\":\"Test\"}"
								.formatted(email, PASSWORD)))
				.andExpect(status().isCreated());

		MvcResult result = mockMvc.perform(post("/login").with(csrf())
						.param("username", email)
						.param("password", PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

}
