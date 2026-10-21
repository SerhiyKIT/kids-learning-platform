package ua.kidlearn.aipipeline;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * The single definition of "a voice line" and of its audio cache key, shared by voicing
 * (VoicingService) and by delivering audio to the child (ScenarioAudioEnricher, AudioController),
 * so a line is always looked up under exactly the hash it was synthesized and stored under.
 */
public final class VoiceLines {

	/** A text hash is lowercase hex SHA-256 — the only shape that may ever reach an object key. */
	public static final Pattern TEXT_HASH = Pattern.compile("^[0-9a-f]{64}$");

	private static final String OBJECT_KEY_PREFIX = "tts/";

	private VoiceLines() {
	}

	/** Visits every {key, text} voiceLine object anywhere in the scenario tree, depth first. */
	public static void forEach(JsonNode node, Consumer<ObjectNode> visitor) {
		if (node instanceof ObjectNode object) {
			JsonNode keyNode = object.get("key");
			JsonNode textNode = object.get("text");
			if (keyNode != null && keyNode.isTextual() && textNode != null && textNode.isTextual()) {
				visitor.accept(object);
			}
		}
		for (JsonNode child : node) {
			forEach(child, visitor);
		}
	}

	/** Distinct voice-line texts in first-seen order. */
	public static Set<String> collectTexts(JsonNode scenario) {
		Set<String> texts = new LinkedHashSet<>();
		forEach(scenario, line -> texts.add(line.get("text").asText()));
		return texts;
	}

	/** audio_assets.text_hash: SHA-256 of voiceId + "\n" + text, lowercase hex. */
	public static String hash(String voiceId, String text) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest((voiceId + "\n" + text).getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hashBytes);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}

	/** Storage object key for a line's audio. Callers must pass an already-validated text hash. */
	public static String objectKey(String textHash) {
		if (!TEXT_HASH.matcher(textHash).matches()) {
			throw new IllegalArgumentException("Not a text hash");
		}
		return OBJECT_KEY_PREFIX + textHash;
	}

	/** Same-origin URL the child-mode player fetches a line's audio from (see AudioController). */
	public static String audioUrl(String textHash) {
		return "/api/audio/" + textHash;
	}

}
