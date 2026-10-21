package ua.kidlearn.aipipeline;

import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import ua.kidlearn.config.TtsProperties;
import ua.kidlearn.lessons.LessonVersion;
import ua.kidlearn.lessons.LessonVersionRepository;

@Service
public class VoicingService {

	/** Only content a human moderator has signed off on gets (paid) TTS. */
	private static final Set<String> VOICEABLE_STATUSES = Set.of(LessonVersion.STATUS_APPROVED,
			LessonVersion.STATUS_PUBLISHED);

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
		if (!VOICEABLE_STATUSES.contains(version.getStatus())) {
			throw new VoicingNotAllowedException(version.getStatus());
		}

		return voiceTexts(VoiceLines.collectTexts(objectMapper.readTree(version.getScenario())));
	}

	/** Voices the guide's fixed child-mode lines (see KidShellLines); already voiced ones are skipped. */
	@Transactional
	public VoicingResult voiceShellLines() {
		return voiceTexts(KidShellLines.texts());
	}

	private VoicingResult voiceTexts(Set<String> texts) {
		String voiceId = ttsProperties.voiceId();
		int synthesized = 0;
		int cached = 0;
		for (String text : texts) {
			String textHash = VoiceLines.hash(voiceId, text);
			if (audioAssetRepository.findByTextHash(textHash).isPresent()) {
				cached++;
				continue;
			}
			TtsAudio audio = ttsProvider.synthesize(text, voiceId);
			String fileUrl = audioStorage.store(VoiceLines.objectKey(textHash), audio.bytes(), audio.contentType());
			audioAssetRepository.save(
					new AudioAsset(textHash, text, ttsProvider.name(), voiceId, fileUrl, audio.durationMs()));
			synthesized++;
		}

		return new VoicingResult(texts.size(), synthesized, cached);
	}

}
