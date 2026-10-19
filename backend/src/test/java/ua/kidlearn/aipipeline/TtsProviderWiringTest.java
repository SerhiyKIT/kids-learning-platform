package ua.kidlearn.aipipeline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import ua.kidlearn.config.TtsProperties;

/**
 * Exactly one TtsProvider is ever wired: the real one only with provider=google AND a non-blank
 * key, the stub in every other combination (incl. CI, which has no key at all).
 */
class TtsProviderWiringTest {

	@Configuration
	@EnableConfigurationProperties(TtsProperties.class)
	static class PropertiesConfig {
	}

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(PropertiesConfig.class, StubTtsProvider.class, GoogleTtsProvider.class)
			.withBean(ObjectMapper.class, () -> JsonMapper.builder().build())
			.withPropertyValues("app.tts.voice-id=default");

	@Test
	void stubWhenNothingIsConfigured() {
		runner.run(context -> {
			assertThat(context).hasSingleBean(TtsProvider.class);
			assertThat(context.getBean(TtsProvider.class)).isInstanceOf(StubTtsProvider.class);
		});
	}

	@Test
	void stubWhenProviderIsGoogleButKeyIsMissing() {
		runner.withPropertyValues("app.tts.provider=google").run(context -> {
			assertThat(context).hasSingleBean(TtsProvider.class);
			assertThat(context.getBean(TtsProvider.class)).isInstanceOf(StubTtsProvider.class);
		});
	}

	@Test
	void stubWhenProviderIsGoogleButKeyIsBlank() {
		runner.withPropertyValues("app.tts.provider=google", "app.tts.api-key=   ").run(context -> {
			assertThat(context).hasSingleBean(TtsProvider.class);
			assertThat(context.getBean(TtsProvider.class)).isInstanceOf(StubTtsProvider.class);
		});
	}

	@Test
	void stubWhenKeyIsSetButProviderIsStub() {
		runner.withPropertyValues("app.tts.provider=stub", "app.tts.api-key=fake-key").run(context -> {
			assertThat(context).hasSingleBean(TtsProvider.class);
			assertThat(context.getBean(TtsProvider.class)).isInstanceOf(StubTtsProvider.class);
		});
	}

	@Test
	void googleWhenProviderIsGoogleAndKeyIsSet() {
		runner.withPropertyValues("app.tts.provider=google", "app.tts.api-key=fake-key").run(context -> {
			assertThat(context).hasSingleBean(TtsProvider.class);
			assertThat(context.getBean(TtsProvider.class)).isInstanceOf(GoogleTtsProvider.class);
			assertThat(context.getBean(TtsProvider.class).name()).isEqualTo("google");
			assertThat(context).doesNotHaveBean(StubTtsProvider.class);
		});
	}

	@Test
	void defaultsMatchTheDocumentedValues() {
		runner.run(context -> {
			TtsProperties properties = context.getBean(TtsProperties.class);
			assertThat(properties.provider()).isEqualTo("stub");
			assertThat(properties.languageCode()).isEqualTo("uk-UA");
			assertThat(properties.audioEncoding()).isEqualTo("MP3");
			assertThat(properties.baseUrl()).isEqualTo("https://texttospeech.googleapis.com");
			assertThat(properties.voiceName()).isNull();
			assertThat(properties.apiKey()).isNull();
		});
	}

}
