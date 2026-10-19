package ua.kidlearn.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * TTS settings. {@code provider}/{@code apiKey}/{@code languageCode}/{@code voiceName} come from
 * the KIDLEARN_TTS_* env vars (see application.yml and infra/.env.example); the key is never
 * committed and has no default. The real provider is only wired when provider=google AND a
 * non-blank key is present — otherwise the deterministic stub stays active (see
 * ua.kidlearn.aipipeline.GoogleTtsCondition).
 *
 * @param voiceId voice identity VoicingService passes to the provider and hashes into the audio
 * cache key (TTS_VOICE_ID; "default" in dev)
 * @param storage audio storage backend ("minio" when unset, "memory" in tests)
 * @param voiceName Google voice name, e.g. one listed by GET /v1/voices?languageCode=uk-UA; blank
 * lets Google pick a voice for {@code languageCode}
 */
@ConfigurationProperties(prefix = "app.tts")
public record TtsProperties(String voiceId, String storage, @DefaultValue("stub") String provider, String apiKey,
		@DefaultValue("uk-UA") String languageCode, String voiceName, @DefaultValue("MP3") String audioEncoding,
		@DefaultValue("https://texttospeech.googleapis.com") String baseUrl,
		@DefaultValue("30s") Duration requestTimeout) {

	// Records get a generated toString() that would print apiKey; keep it out of any log line.
	@Override
	public String toString() {
		return "TtsProperties[voiceId=" + voiceId + ", storage=" + storage + ", provider=" + provider + ", apiKey="
				+ (apiKey == null || apiKey.isBlank() ? "<unset>" : "<redacted>") + ", languageCode=" + languageCode
				+ ", voiceName=" + voiceName + ", audioEncoding=" + audioEncoding + ", baseUrl=" + baseUrl
				+ ", requestTimeout=" + requestTimeout + "]";
	}

}
