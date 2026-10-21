package ua.kidlearn.aipipeline;

import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

/**
 * Deterministic fake TTS: no network, no credentials, same input always produces the same output.
 * Active whenever the real provider isn't (see {@link GoogleTtsCondition}): the default in dev and
 * the only provider the test suite and CI ever see — mirroring how
 * MinioAudioStorage/InMemoryAudioStorage are split between real and test wiring.
 *
 * <p>Returns a real, decodable MP3 of silence (MPEG-1 Layer III frames with empty audio data)
 * whose length matches the reported duration, so the child-mode player can genuinely load and
 * play stub-voiced lessons end to end in dev — just without anything audible.
 */
@Component
@Conditional(GoogleTtsCondition.Inverse.class)
class StubTtsProvider implements TtsProvider {

	private static final int MS_PER_CHARACTER = 60;
	private static final int MIN_DURATION_MS = 500;

	// MPEG-1 Layer III, no CRC, 128 kbps, 44.1 kHz, mono: 144 * 128000 / 44100 = 417 bytes/frame,
	// 1152 samples/frame. All-zero side info and main data decode as silence.
	private static final byte[] FRAME_HEADER = { (byte) 0xFF, (byte) 0xFB, (byte) 0x90, (byte) 0xC0 };
	private static final int FRAME_BYTES = 417;
	private static final double FRAME_MS = 1152 * 1000.0 / 44100;

	@Override
	public String name() {
		return "stub";
	}

	@Override
	public TtsAudio synthesize(String text, String voiceId) {
		int durationMs = Math.max(MIN_DURATION_MS, text.length() * MS_PER_CHARACTER);
		return new TtsAudio(silentMp3(durationMs), "audio/mpeg", durationMs);
	}

	static byte[] silentMp3(int durationMs) {
		int frames = (int) Math.ceil(durationMs / FRAME_MS);
		byte[] mp3 = new byte[frames * FRAME_BYTES];
		for (int i = 0; i < frames; i++) {
			System.arraycopy(FRAME_HEADER, 0, mp3, i * FRAME_BYTES, FRAME_HEADER.length);
		}
		return mp3;
	}

}
