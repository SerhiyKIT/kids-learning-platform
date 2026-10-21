package ua.kidlearn.aipipeline;

import java.time.Duration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

/**
 * Serves synthesized voice-line audio to any signed-in user (anonymous → 401, like the rest of
 * /api). Audio is lesson content, not child data, so there's no ownership check and no audit.
 * Content-addressed by text hash, hence cacheable forever. MinIO itself stays private: bytes are
 * proxied through here, and the path parameter only ever reaches the object key after it has been
 * validated as a 64-char lowercase hex hash.
 */
@RestController
public class AudioController {

	private static final CacheControl FOREVER = CacheControl.maxAge(Duration.ofDays(365)).cachePrivate().immutable();
	private static final MediaType DEFAULT_AUDIO_TYPE = MediaType.parseMediaType("audio/mpeg");

	private final AudioDeliveryService audioDeliveryService;

	public AudioController(AudioDeliveryService audioDeliveryService) {
		this.audioDeliveryService = audioDeliveryService;
	}

	@GetMapping("/api/audio/{textHash}")
	public ResponseEntity<Resource> audio(@PathVariable String textHash, WebRequest request) {
		if (!VoiceLines.TEXT_HASH.matcher(textHash).matches()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid audio id");
		}
		String etag = "\"" + textHash + "\"";
		if (!audioDeliveryService.exists(textHash)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Audio not found");
		}
		// Content-addressed: if the client already has this hash, the bytes can't have changed.
		if (request.checkNotModified(etag)) {
			return null;
		}
		StoredAudio audio = audioDeliveryService.load(textHash)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audio not found"));
		// A Resource body also gets HTTP Range support (206) from Spring MVC for free.
		return ResponseEntity.ok()
				.contentType(contentType(audio.contentType()))
				.contentLength(audio.bytes().length)
				.eTag(etag)
				.cacheControl(FOREVER)
				.body(new ByteArrayResource(audio.bytes()));
	}

	private static MediaType contentType(String stored) {
		try {
			return stored == null || stored.isBlank() ? DEFAULT_AUDIO_TYPE : MediaType.parseMediaType(stored);
		} catch (IllegalArgumentException e) {
			return DEFAULT_AUDIO_TYPE;
		}
	}

}
