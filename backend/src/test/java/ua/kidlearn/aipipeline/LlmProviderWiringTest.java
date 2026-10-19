package ua.kidlearn.aipipeline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import ua.kidlearn.config.LlmProperties;

/**
 * Exactly one LlmProvider is ever wired: the real one only with provider=anthropic AND a
 * non-blank key, the stub in every other combination (incl. CI, which has no key at all).
 */
class LlmProviderWiringTest {

	@Configuration
	@EnableConfigurationProperties(LlmProperties.class)
	static class PropertiesConfig {
	}

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(PropertiesConfig.class, StubLlmProvider.class, AnthropicLlmProvider.class)
			.withBean(ObjectMapper.class, () -> JsonMapper.builder().build())
			.withPropertyValues("app.llm.max-attempts=2");

	@Test
	void stubWhenNothingIsConfigured() {
		runner.run(context -> {
			assertThat(context).hasSingleBean(LlmProvider.class);
			assertThat(context.getBean(LlmProvider.class)).isInstanceOf(StubLlmProvider.class);
		});
	}

	@Test
	void stubWhenProviderIsAnthropicButKeyIsMissing() {
		runner.withPropertyValues("app.llm.provider=anthropic").run(context -> {
			assertThat(context).hasSingleBean(LlmProvider.class);
			assertThat(context.getBean(LlmProvider.class)).isInstanceOf(StubLlmProvider.class);
		});
	}

	@Test
	void stubWhenProviderIsAnthropicButKeyIsBlank() {
		runner.withPropertyValues("app.llm.provider=anthropic", "app.llm.api-key=   ").run(context -> {
			assertThat(context).hasSingleBean(LlmProvider.class);
			assertThat(context.getBean(LlmProvider.class)).isInstanceOf(StubLlmProvider.class);
		});
	}

	@Test
	void stubWhenKeyIsSetButProviderIsStub() {
		runner.withPropertyValues("app.llm.provider=stub", "app.llm.api-key=fake-key").run(context -> {
			assertThat(context).hasSingleBean(LlmProvider.class);
			assertThat(context.getBean(LlmProvider.class)).isInstanceOf(StubLlmProvider.class);
		});
	}

	@Test
	void anthropicWhenProviderIsAnthropicAndKeyIsSet() {
		runner.withPropertyValues("app.llm.provider=anthropic", "app.llm.api-key=fake-key").run(context -> {
			assertThat(context).hasSingleBean(LlmProvider.class);
			assertThat(context.getBean(LlmProvider.class)).isInstanceOf(AnthropicLlmProvider.class);
			assertThat(context).doesNotHaveBean(StubLlmProvider.class);
		});
	}

	@Test
	void defaultsMatchTheDocumentedValues() {
		runner.run(context -> {
			LlmProperties properties = context.getBean(LlmProperties.class);
			assertThat(properties.provider()).isEqualTo("stub");
			assertThat(properties.model()).isEqualTo("claude-sonnet-5-5");
			assertThat(properties.baseUrl()).isEqualTo("https://api.anthropic.com");
			assertThat(properties.apiKey()).isNull();
		});
	}

}
