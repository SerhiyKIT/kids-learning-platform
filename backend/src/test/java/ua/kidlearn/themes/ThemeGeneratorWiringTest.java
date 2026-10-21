package ua.kidlearn.themes;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import ua.kidlearn.config.LlmProperties;

/** Same switch as the lesson LLM: the real theme generator only with provider=anthropic AND a key. */
class ThemeGeneratorWiringTest {

	@Configuration
	@EnableConfigurationProperties(LlmProperties.class)
	static class PropertiesConfig {
	}

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(PropertiesConfig.class, StubThemeGenerator.class, AnthropicThemeGenerator.class)
			.withBean(ObjectMapper.class, () -> JsonMapper.builder().build())
			.withPropertyValues("app.llm.max-attempts=2");

	@Test
	void stubWithoutProviderOrWithoutKey() {
		for (String[] props : new String[][] {{}, {"app.llm.provider=anthropic"},
				{"app.llm.provider=anthropic", "app.llm.api-key=  "}, {"app.llm.provider=stub", "app.llm.api-key=k"}}) {
			runner.withPropertyValues(props).run(context -> {
				assertThat(context).hasSingleBean(ThemeGenerator.class);
				assertThat(context.getBean(ThemeGenerator.class)).isInstanceOf(StubThemeGenerator.class);
			});
		}
	}

	@Test
	void anthropicWithProviderAndKey() {
		runner.withPropertyValues("app.llm.provider=anthropic", "app.llm.api-key=fake-key").run(context -> {
			assertThat(context).hasSingleBean(ThemeGenerator.class);
			assertThat(context.getBean(ThemeGenerator.class)).isInstanceOf(AnthropicThemeGenerator.class);
		});
	}

}
