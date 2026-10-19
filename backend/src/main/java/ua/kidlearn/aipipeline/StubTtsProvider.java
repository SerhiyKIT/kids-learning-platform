package ua.kidlearn.aipipeline;

import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

/**
 * Deterministic fake TTS: no network, no credentials, same input always produces the same output.
 * Active whenever the real provider isn't (see {@link GoogleTtsCondition}): the default in dev and
 * the only provider the test suite and CI ever see — mirroring how
 * MinioAudioStorage/InMemoryAudioStorage are split between real and test wiring.
 */
@Component
@Conditional(GoogleTtsCondition.Inverse.class)
class StubTtsProvider implements TtsProvider {

	private static final byte[] FIXED_AUDIO_BYTES = { 0x00, 0x01, 0x02, 0x03 };
	private static final int MS_PER_CHARACTER = 60;
	private static final int MIN_DURATION_MS = 500;

	@Override
	public String name() {
		return "stub";
	}

	@Override
	public TtsAudio synthesize(String text, String voiceId) {
		int durationMs = Math.max(MIN_DURATION_MS, text.length() * MS_PER_CHARACTER);
		return new TtsAudio(FIXED_AUDIO_BYTES, "audio/mpeg", durationMs);
	}

}
