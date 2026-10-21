package ua.kidlearn.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ua.kidlearn.auth.AuthMailSender;

/**
 * The one shared configuration of full-stack tests (dev profile + MockMvc). Every test class
 * that extends it, directly or through ClockedWebIntegrationTest / InMemoryAudioWebIntegrationTest,
 * reuses a cached Spring context instead of starting its own — each context holds its own
 * connection pool, and too many of them used to exhaust Postgres' max_connections.
 *
 * <ul>
 * <li>Mail is a mock for everyone: no test talks to SMTP; tests that check emails verify it.</li>
 * <li>The first-admin bootstrap token is configured (the not-configured case runs in the
 * prod-like "guardtest" context, see BootstrapNotConfiguredTest).</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = "app.bootstrap.admin-token=" + WebIntegrationTest.BOOTSTRAP_TOKEN)
public abstract class WebIntegrationTest {

	public static final String BOOTSTRAP_TOKEN = "correct-horse-battery-staple";

	@MockitoBean
	protected AuthMailSender authMailSender;

}
