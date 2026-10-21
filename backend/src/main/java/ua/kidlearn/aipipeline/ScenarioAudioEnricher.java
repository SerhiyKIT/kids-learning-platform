package ua.kidlearn.aipipeline;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import ua.kidlearn.config.TtsProperties;

/**
 * Adds "audioUrl" to every voice line of a scenario that has synthesized audio, for the child-mode
 * player. Works on a copy — the stored scenario (and therefore everything ScenarioValidator ever
 * checks) is never modified; audioUrl exists only in this response. A line is looked up under the
 * hash for the current TTS voice; if there's none (e.g. the voice changed after voicing), any
 * audio already recorded for the same text is used instead. Lines without audio stay as they are.
 */
@Component
public class ScenarioAudioEnricher {

	private final AudioAssetRepository audioAssetRepository;
	private final TtsProperties ttsProperties;

	public ScenarioAudioEnricher(AudioAssetRepository audioAssetRepository, TtsProperties ttsProperties) {
		this.audioAssetRepository = audioAssetRepository;
		this.ttsProperties = ttsProperties;
	}

	public JsonNode withAudioUrls(JsonNode scenario) {
		JsonNode enriched = scenario.deepCopy();
		Set<String> texts = VoiceLines.collectTexts(enriched);
		if (texts.isEmpty()) {
			return enriched;
		}
		String voiceId = ttsProperties.voiceId();
		Map<String, String> currentVoiceHashByText = texts.stream()
				.collect(Collectors.toMap(t -> t, t -> VoiceLines.hash(voiceId, t)));

		List<AudioAsset> assets = audioAssetRepository.findByTextHashInOrTextIn(currentVoiceHashByText.values(), texts);
		Set<String> existingHashes = assets.stream().map(AudioAsset::getTextHash).collect(Collectors.toSet());
		// Deterministic fallback: the lowest hash among the same text's assets.
		Map<String, String> fallbackHashByText = new HashMap<>();
		assets.stream()
				.sorted(Comparator.comparing(AudioAsset::getTextHash))
				.forEach(a -> fallbackHashByText.putIfAbsent(a.getText(), a.getTextHash()));

		VoiceLines.forEach(enriched, line -> {
			String text = line.get("text").asText();
			String hash = currentVoiceHashByText.get(text);
			String audioHash = existingHashes.contains(hash) ? hash : fallbackHashByText.get(text);
			if (audioHash != null) {
				line.put("audioUrl", VoiceLines.audioUrl(audioHash));
			}
		});
		return enriched;
	}

}
