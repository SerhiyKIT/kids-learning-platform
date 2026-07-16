package ua.kidlearn.aipipeline;

import org.springframework.stereotype.Component;

/**
 * Deterministic fake TTS: no network, no credentials, same input always
 * produces the same output. The only implementation for now — final
 * provider choice (ElevenLabs / Azure / Google / Polly) is pending the
 * blind voice test (docs/Формат_подання_уроків_та_ШІ.md §8).
 *
 * TODO: add a real provider implementation once that test picks one, and
 * make it the active TtsProvider in dev/prod (this stub would then move to
 * test-only wiring, mirroring how MinioAudioStorage/InMemoryAudioStorage
 * are split).
 */
@Component
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
