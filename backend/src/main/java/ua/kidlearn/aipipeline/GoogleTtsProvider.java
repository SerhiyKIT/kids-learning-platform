package ua.kidlearn.aipipeline;

import java.net.http.HttpClient;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import ua.kidlearn.config.TtsProperties;

/**
 * Real TTS on Google Cloud Text-to-Speech (POST /v1/text:synthesize), over plain RestClient rather
 * than the Google SDK to keep dependencies minimal. Active only when app.tts.provider=google and
 * an API key is set ({@link GoogleTtsCondition}); otherwise StubTtsProvider is wired instead.
 *
 * <p>Only synthesizes: caching by text_hash and storage stay in VoicingService/AudioStorage. The
 * key goes in the x-goog-api-key header, never the URL
 * (https://docs.cloud.google.com/docs/authentication/api-keys-use).
 *
 * <p>Only MP3 is supported for now: it's the one encoding whose content type and bitrate (and so
 * duration) are documented as fixed — "MP3 audio at 32kbps"
 * (https://docs.cloud.google.com/text-to-speech/docs/reference/rest/Shared.Types/AudioEncoding).
 */
@Component
@Conditional(GoogleTtsCondition.class)
class GoogleTtsProvider implements TtsProvider {

	private static final Logger log = LoggerFactory.getLogger(GoogleTtsProvider.class);

	private static final String SUPPORTED_ENCODING = "MP3";
	private static final String MP3_CONTENT_TYPE = "audio/mpeg";
	private static final int MP3_BITS_PER_MS = 32; // 32 kbps

	private final TtsProperties properties;
	private final ObjectMapper objectMapper;
	private final RestClient restClient;

	@Autowired
	GoogleTtsProvider(TtsProperties properties, ObjectMapper objectMapper) {
		this(properties, objectMapper, RestClient.builder().requestFactory(requestFactory(properties)));
	}

	/** Test seam: lets a test bind MockRestServiceServer to {@code restClientBuilder}. */
	GoogleTtsProvider(TtsProperties properties, ObjectMapper objectMapper, RestClient.Builder restClientBuilder) {
		if (!SUPPORTED_ENCODING.equalsIgnoreCase(properties.audioEncoding())) {
			throw new IllegalStateException("app.tts.audio-encoding=" + properties.audioEncoding()
					+ " is not supported; only " + SUPPORTED_ENCODING + " is");
		}
		this.properties = properties;
		this.objectMapper = objectMapper;
		// Built here, not on first call, but construction still does no network I/O.
		this.restClient = restClientBuilder.baseUrl(properties.baseUrl())
				.defaultHeader("x-goog-api-key", properties.apiKey())
				.build();
	}

	private static JdkClientHttpRequestFactory requestFactory(TtsProperties properties) {
		HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.requestTimeout()).build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(properties.requestTimeout());
		return requestFactory;
	}

	@Override
	public String name() {
		return GoogleTtsCondition.PROVIDER_NAME;
	}

	@Override
	public TtsAudio synthesize(String text, String voiceId) {
		String voiceName = resolveVoiceName(voiceId);
		log.info("Requesting Google TTS: languageCode={}, voice={}, textLength={}", properties.languageCode(),
				voiceName == null ? "<auto>" : voiceName, text.length());
		String responseBody = post(objectMapper.writeValueAsString(buildRequestBody(text, voiceName)));
		return parseResponse(responseBody);
	}

	/**
	 * The configured voice name wins; otherwise {@code voiceId} is used only if it's a voice of the
	 * configured language (VoicingService passes the cache-key voice id, "default" in dev, which
	 * is not a Google voice). With neither, the name is omitted and Google picks a voice for the
	 * language (VoiceSelectionParams: "the service will choose a voice based on ... languageCode").
	 */
	String resolveVoiceName(String voiceId) {
		if (StringUtils.hasText(properties.voiceName())) {
			return properties.voiceName().strip();
		}
		if (voiceId != null && voiceId.startsWith(properties.languageCode() + "-")) {
			return voiceId;
		}
		return null;
	}

	ObjectNode buildRequestBody(String text, String voiceName) {
		ObjectNode body = objectMapper.createObjectNode();
		body.putObject("input").put("text", text);
		ObjectNode voice = body.putObject("voice").put("languageCode", properties.languageCode());
		if (voiceName != null) {
			voice.put("name", voiceName);
		}
		body.putObject("audioConfig").put("audioEncoding", SUPPORTED_ENCODING);
		return body;
	}

	private String post(String requestBody) {
		try {
			return restClient.post()
					.uri("/v1/text:synthesize")
					.contentType(MediaType.APPLICATION_JSON)
					.body(requestBody)
					.retrieve()
					.body(String.class);
		} catch (RestClientResponseException e) {
			// Status + Google's error status only; the error message may echo request content.
			String errorStatus = errorStatus(e.getResponseBodyAsString());
			log.warn("Google TTS returned {} ({})", e.getStatusCode().value(), errorStatus);
			throw new TtsSynthesisException(TtsSynthesisException.Reason.UPSTREAM_ERROR,
					"Google TTS returned " + e.getStatusCode().value() + " (" + errorStatus + ")", e);
		} catch (ResourceAccessException e) {
			log.warn("Google TTS unreachable or timed out: {}", e.getClass().getSimpleName());
			throw new TtsSynthesisException(TtsSynthesisException.Reason.UNAVAILABLE,
					"Google TTS unreachable or timed out", e);
		} catch (RestClientException e) {
			log.warn("Google TTS call failed: {}", e.getClass().getSimpleName());
			throw new TtsSynthesisException(TtsSynthesisException.Reason.UPSTREAM_ERROR, "Google TTS call failed", e);
		}
	}

	TtsAudio parseResponse(String responseBody) {
		JsonNode response;
		try {
			response = objectMapper.readTree(responseBody == null ? "" : responseBody);
		} catch (JacksonException e) {
			throw new TtsSynthesisException(TtsSynthesisException.Reason.UPSTREAM_ERROR,
					"Google TTS returned a non-JSON body", e);
		}
		// JSON carries the audio bytes base64-encoded (text.synthesize response: audioContent).
		String audioContent = response.path("audioContent").asText("");
		if (audioContent.isBlank()) {
			throw new TtsSynthesisException(TtsSynthesisException.Reason.EMPTY_AUDIO,
					"Google TTS response contained no audioContent");
		}
		byte[] bytes;
		try {
			bytes = Base64.getDecoder().decode(audioContent);
		} catch (IllegalArgumentException e) {
			throw new TtsSynthesisException(TtsSynthesisException.Reason.UPSTREAM_ERROR,
					"Google TTS audioContent is not valid base64", e);
		}
		if (bytes.length == 0) {
			throw new TtsSynthesisException(TtsSynthesisException.Reason.EMPTY_AUDIO,
					"Google TTS response contained no audioContent");
		}
		return new TtsAudio(bytes, MP3_CONTENT_TYPE, durationMs(bytes.length));
	}

	/** Constant-bitrate estimate: bytes * 8 bits / 32 bits-per-ms. */
	static int durationMs(int mp3Bytes) {
		return (int) ((long) mp3Bytes * 8 / MP3_BITS_PER_MS);
	}

	private String errorStatus(String errorBody) {
		try {
			return objectMapper.readTree(errorBody).path("error").path("status").asText("unknown");
		} catch (JacksonException e) {
			return "unknown";
		}
	}

}
