package ua.kidlearn.aipipeline;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import ua.kidlearn.config.TtsProperties;
import ua.kidlearn.lessons.LessonVersion;
import ua.kidlearn.lessons.LessonVersionRepository;

@Service
public class VoicingService {

	private final LessonVersionRepository lessonVersionRepository;
	private final AudioAssetRepository audioAssetRepository;
	private final TtsProvider ttsProvider;
	private final AudioStorage audioStorage;
	private final ObjectMapper objectMapper;
	private final TtsProperties ttsProperties;

	public VoicingService(LessonVersionRepository lessonVersionRepository, AudioAssetRepository audioAssetRepository,
			TtsProvider ttsProvider, AudioStorage audioStorage, ObjectMapper objectMapper,
			TtsProperties ttsProperties) {
		this.lessonVersionRepository = lessonVersionRepository;
		this.audioAssetRepository = audioAssetRepository;
		this.ttsProvider = ttsProvider;
		this.audioStorage = audioStorage;
		this.objectMapper = objectMapper;
		this.ttsProperties = ttsProperties;
	}

	@Transactional
	public VoicingResult voice(UUID lessonVersionId) {
		LessonVersion version = lessonVersionRepository.findById(lessonVersionId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson version not found"));

		Set<String> texts = new LinkedHashSet<>();
		collectVoiceLineTexts(objectMapper.readTree(version.getScenario()), texts);

		String voiceId = ttsProperties.voiceId();
		int synthesized = 0;
		int cached = 0;
		for (String text : texts) {
			String textHash = hash(voiceId, text);
			if (audioAssetRepository.findByTextHash(textHash).isPresent()) {
				cached++;
				continue;
			}
			TtsAudio audio = ttsProvider.synthesize(text, voiceId);
			String fileUrl = audioStorage.store("tts/" + textHash, audio.bytes(), audio.contentType());
			audioAssetRepository.save(
					new AudioAsset(textHash, text, ttsProvider.name(), voiceId, fileUrl, audio.durationMs()));
			synthesized++;
		}

		return new VoicingResult(texts.size(), synthesized, cached);
	}

	/** Recursively collects distinct texts from every {key,text} voiceLine object in the tree. */
	private void collectVoiceLineTexts(JsonNode node, Set<String> texts) {
		if (node.isObject()) {
			JsonNode keyNode = node.get("key");
			JsonNode textNode = node.get("text");
			if (keyNode != null && keyNode.isTextual() && textNode != null && textNode.isTextual()) {
				texts.add(textNode.asText());
			}
		}
		for (JsonNode child : node) {
			collectVoiceLineTexts(child, texts);
		}
	}

	private String hash(String voiceId, String text) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest((voiceId + "\n" + text).getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hashBytes);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}

}
