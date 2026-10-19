package ua.kidlearn.aipipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import ua.kidlearn.config.TtsProperties;

/** GoogleTtsProvider against MockRestServiceServer — no network, no real key. */
class GoogleTtsProviderTest {

	private static final String FAKE_KEY = "test-key-not-a-real-secret";
	private static final String BASE_URL = "https://tts.example.test";
	private static final String SYNTHESIZE_URL = BASE_URL + "/v1/text:synthesize";

	/** Not a real MP3, just distinctive bytes to prove base64 decoding round-trips exactly. */
	private static final byte[] AUDIO_BYTES = { (byte) 0xFF, (byte) 0xFB, 0x10, 0x00, 0x7F, 0x00, 0x01, 0x02 };

	private final ObjectMapper objectMapper = JsonMapper.builder().build();
	private final RestClient.Builder restClientBuilder = RestClient.builder();
	private final MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();

	private static TtsProperties properties(String voiceName, String audioEncoding) {
		return new TtsProperties("default", null, "google", FAKE_KEY, "uk-UA", voiceName, audioEncoding, BASE_URL,
				Duration.ofSeconds(5));
	}

	private GoogleTtsProvider provider(String voiceName) {
		return new GoogleTtsProvider(properties(voiceName, "MP3"), objectMapper, restClientBuilder);
	}

	private static String audioResponse(byte[] bytes) {
		return "{\"audioContent\":\"" + Base64.getEncoder().encodeToString(bytes) + "\"}";
	}

	@Test
	void base64AudioContentIsDecodedIntoMp3TtsAudio() {
		server.expect(requestTo(SYNTHESIZE_URL))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("x-goog-api-key", FAKE_KEY))
				.andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
				.andExpect(content().json("""
						{"input":{"text":"Привіт, друже!"},
						 "voice":{"languageCode":"uk-UA","name":"uk-UA-Wavenet-B"},
						 "audioConfig":{"audioEncoding":"MP3"}}
						""", JsonCompareMode.STRICT))
				.andRespond(withSuccess(audioResponse(AUDIO_BYTES), MediaType.APPLICATION_JSON));

		TtsAudio audio = provider("uk-UA-Wavenet-B").synthesize("Привіт, друже!", "default");

		server.verify();
		assertThat(audio.bytes()).containsExactly(AUDIO_BYTES);
		assertThat(audio.contentType()).isEqualTo("audio/mpeg");
		assertThat(audio.durationMs()).isEqualTo(GoogleTtsProvider.durationMs(AUDIO_BYTES.length));
	}

	@Test
	void voiceNameIsOmittedWhenNeitherConfiguredNorAGoogleVoiceId() {
		server.expect(requestTo(SYNTHESIZE_URL))
				.andExpect(content().json("""
						{"input":{"text":"Так!"},"voice":{"languageCode":"uk-UA"},"audioConfig":{"audioEncoding":"MP3"}}
						""", JsonCompareMode.STRICT))
				.andRespond(withSuccess(audioResponse(AUDIO_BYTES), MediaType.APPLICATION_JSON));

		provider(null).synthesize("Так!", "default");

		server.verify();
	}

	@Test
	void voiceIdIsUsedWhenItIsAVoiceOfTheConfiguredLanguage() {
		GoogleTtsProvider provider = provider("");

		assertThat(provider.resolveVoiceName("uk-UA-Standard-B")).isEqualTo("uk-UA-Standard-B");
		assertThat(provider.resolveVoiceName("default")).isNull();
		assertThat(provider.resolveVoiceName("en-US-Standard-A")).isNull();
		assertThat(provider("uk-UA-Wavenet-B").resolveVoiceName("uk-UA-Standard-B")).isEqualTo("uk-UA-Wavenet-B");
	}

	@Test
	void mp3DurationFollowsTheDocumented32kbpsBitrate() {
		// 32 kbps = 4000 bytes per second.
		assertThat(GoogleTtsProvider.durationMs(4000)).isEqualTo(1000);
		assertThat(GoogleTtsProvider.durationMs(10_000)).isEqualTo(2500);
	}

	@Test
	void non2xxBecomesUpstreamErrorWithoutLeakingTheKey() {
		server.expect(requestTo(SYNTHESIZE_URL))
				.andRespond(withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON).body("""
						{"error":{"code":403,"message":"API key not valid.","status":"PERMISSION_DENIED"}}
						"""));

		assertThatThrownBy(() -> provider(null).synthesize("Так!", "default"))
				.isInstanceOfSatisfying(TtsSynthesisException.class, e -> {
					assertThat(e.reason()).isEqualTo(TtsSynthesisException.Reason.UPSTREAM_ERROR);
					assertThat(e.getMessage()).contains("403", "PERMISSION_DENIED").doesNotContain(FAKE_KEY);
				});
	}

	@Test
	void serverErrorBecomesUpstreamError() {
		server.expect(requestTo(SYNTHESIZE_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

		assertThatThrownBy(() -> provider(null).synthesize("Так!", "default"))
				.isInstanceOfSatisfying(TtsSynthesisException.class,
						e -> assertThat(e.reason()).isEqualTo(TtsSynthesisException.Reason.UPSTREAM_ERROR));
	}

	@Test
	void missingOrEmptyAudioContentBecomesEmptyAudio() {
		server.expect(requestTo(SYNTHESIZE_URL)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
		server.expect(requestTo(SYNTHESIZE_URL))
				.andRespond(withSuccess("{\"audioContent\":\"\"}", MediaType.APPLICATION_JSON));
		GoogleTtsProvider provider = provider(null);

		for (int i = 0; i < 2; i++) {
			assertThatThrownBy(() -> provider.synthesize("Так!", "default"))
					.isInstanceOfSatisfying(TtsSynthesisException.class,
							e -> assertThat(e.reason()).isEqualTo(TtsSynthesisException.Reason.EMPTY_AUDIO));
		}
		server.verify();
	}

	@Test
	void invalidBase64BecomesUpstreamError() {
		server.expect(requestTo(SYNTHESIZE_URL))
				.andRespond(withSuccess("{\"audioContent\":\"not base64 !!\"}", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> provider(null).synthesize("Так!", "default"))
				.isInstanceOfSatisfying(TtsSynthesisException.class,
						e -> assertThat(e.reason()).isEqualTo(TtsSynthesisException.Reason.UPSTREAM_ERROR));
	}

	@Test
	void timeoutBecomesUnavailable() {
		server.expect(requestTo(SYNTHESIZE_URL)).andRespond(withException(new SocketTimeoutException("timed out")));

		assertThatThrownBy(() -> provider(null).synthesize("Так!", "default"))
				.isInstanceOfSatisfying(TtsSynthesisException.class,
						e -> assertThat(e.reason()).isEqualTo(TtsSynthesisException.Reason.UNAVAILABLE));
	}

	@Test
	void unsupportedAudioEncodingFailsFastAtConstruction() {
		assertThatThrownBy(() -> new GoogleTtsProvider(properties(null, "OGG_OPUS"), objectMapper, restClientBuilder))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("OGG_OPUS");
	}

	@Test
	void apiKeyNeverAppearsInPropertiesToString() {
		assertThat(properties(null, "MP3").toString()).doesNotContain(FAKE_KEY).contains("<redacted>");
	}

}
