package ua.kidlearn.aipipeline;

import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Looks up a voice line's audio by text hash for AudioController (which may not touch repositories). */
@Service
public class AudioDeliveryService {

	private final AudioAssetRepository audioAssetRepository;
	private final AudioStorage audioStorage;

	public AudioDeliveryService(AudioAssetRepository audioAssetRepository, AudioStorage audioStorage) {
		this.audioAssetRepository = audioAssetRepository;
		this.audioStorage = audioStorage;
	}

	/** Whether an audio_assets row exists for this (already validated) hash — cheap, no storage I/O. */
	@Transactional(readOnly = true)
	public boolean exists(String textHash) {
		return audioAssetRepository.findByTextHash(textHash).isPresent();
	}

	/** The stored bytes for this (already validated) hash; empty if the row or the object is missing. */
	@Transactional(readOnly = true)
	public Optional<StoredAudio> load(String textHash) {
		if (!exists(textHash)) {
			return Optional.empty();
		}
		return audioStorage.load(VoiceLines.objectKey(textHash));
	}

}
