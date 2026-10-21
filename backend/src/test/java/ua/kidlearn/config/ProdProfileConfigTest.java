package ua.kidlearn.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

/**
 * The production profile's hardening, read straight from application-prod.yml (it can't boot in a
 * test: every secret comes from env with no default — which is itself checked here).
 */
class ProdProfileConfigTest {

	private static PropertySource<?> load(String name) throws IOException {
		return new YamlPropertySourceLoader().load(name, new ClassPathResource(name)).getFirst();
	}

	@Test
	void cookiesAreSecureHttpOnlyLaxAndForwardedHeadersAreHonoured() throws IOException {
		PropertySource<?> prod = load("application-prod.yml");
		assertThat(prod.getProperty("server.servlet.session.cookie.secure")).isEqualTo(true);
		assertThat(prod.getProperty("server.servlet.session.cookie.http-only")).isEqualTo(true);
		assertThat(prod.getProperty("server.servlet.session.cookie.same-site")).isEqualTo("lax");
		assertThat(prod.getProperty("server.forward-headers-strategy")).isEqualTo("native");
		assertThat(prod.getProperty("app.security.secure-cookies")).isEqualTo(true);
		assertThat(prod.getProperty("app.ratelimit.trust-forwarded-header")).isEqualTo(true);
	}

	@Test
	void secretsHaveNoDefaultsInProd() throws IOException {
		PropertySource<?> prod = load("application-prod.yml");
		for (String key : new String[] {"spring.datasource.password", "spring.mail.password", "app.twofa.enc-key",
				"minio.secret-key", "app.base-url", "app.mail-from"}) {
			assertThat(String.valueOf(prod.getProperty(key))).as(key).matches(Pattern.compile("^\\$\\{[A-Z_]+}$"));
		}
	}

	@Test
	void forwardedHeadersAreIgnoredUnlessAProfileSaysOtherwiseAndActuatorHidesDetails() throws IOException {
		PropertySource<?> base = load("application.yml");
		assertThat(base.getProperty("app.ratelimit.trust-forwarded-header")).isEqualTo(false);
		assertThat(base.getProperty("management.endpoints.web.exposure.include")).isEqualTo("health,info");
		assertThat(base.getProperty("management.endpoint.health.show-details")).isEqualTo("never");
	}

}
