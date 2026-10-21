package ua.kidlearn.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ProfileGuardTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(ProfileGuard.class);

	@Test
	void prodTogetherWithADevProfileRefusesToStart() {
		assertThatThrownBy(() -> ProfileGuard.check("prod", "dev")).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("[dev]");
		assertThatThrownBy(() -> ProfileGuard.check("dev", "prod")).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> ProfileGuard.check("prod", "guardtest")).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> ProfileGuard.check("prod", "e2e")).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void prodAloneDevAloneAndNoProfileAreFine() {
		assertThatCode(() -> ProfileGuard.check("prod")).doesNotThrowAnyException();
		assertThatCode(() -> ProfileGuard.check("dev")).doesNotThrowAnyException();
		assertThatCode(ProfileGuard::check).doesNotThrowAnyException();
	}

	@Test
	void theContextItselfFailsBeforeAnyBeanIsCreated() {
		runner.withInitializer(context -> context.getEnvironment().setActiveProfiles("prod", "dev"))
				.run(context -> assertThat(context).hasFailed()
						.getFailure().hasMessageContaining("must not run together"));
		runner.withInitializer(context -> context.getEnvironment().setActiveProfiles("prod"))
				.run(context -> assertThat(context).hasNotFailed());
	}

}
