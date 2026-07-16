package ua.kidlearn.aipipeline;

public interface TtsProvider {

	/** Provider identifier stored alongside each cached {@link AudioAsset}. */
	String name();

	TtsAudio synthesize(String text, String voiceId);

}
