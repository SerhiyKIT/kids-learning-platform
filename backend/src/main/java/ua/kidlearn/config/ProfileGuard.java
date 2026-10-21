package ua.kidlearn.config;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Refuses to start production together with a profile that brings dev-only beans
 * (@Profile("dev"): account creation for any role, demo seeding) or dev fallbacks (a committed
 * 2FA key, default passwords). Runs as a BeanFactoryPostProcessor, i.e. before any application
 * bean exists — the context fails to start, nothing is served.
 */
@Component
public class ProfileGuard implements BeanFactoryPostProcessor, EnvironmentAware {

	/** Profiles that must never be active alongside "prod". */
	static final Set<String> DEV_ONLY_PROFILES = Set.of("dev", "guardtest", "e2e");

	private Environment environment;

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		check(environment.getActiveProfiles());
	}

	static void check(String... activeProfiles) {
		List<String> active = Arrays.asList(activeProfiles);
		if (!active.contains("prod")) {
			return;
		}
		List<String> clashing = active.stream().filter(DEV_ONLY_PROFILES::contains).toList();
		if (!clashing.isEmpty()) {
			throw new IllegalStateException("Profile 'prod' must not run together with " + clashing
					+ " (dev-only beans and fallbacks); active profiles: " + active);
		}
	}

}
