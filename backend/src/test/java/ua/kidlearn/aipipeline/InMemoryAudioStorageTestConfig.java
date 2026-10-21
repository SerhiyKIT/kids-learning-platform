package ua.kidlearn.aipipeline;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Swaps in a stateful in-memory AudioStorage so tests never touch MinIO.
 * Combined with @TestPropertySource(properties = "app.tts.storage=memory")
 * on the test class, which stops MinioAudioStorage (@ConditionalOnProperty)
 * from being instantiated at all.
 */
@TestConfiguration
public class InMemoryAudioStorageTestConfig {

	@Bean
	AudioStorage audioStorage() {
		return new InMemoryAudioStorage();
	}

	static final class InMemoryAudioStorage implements AudioStorage {

		private final Map<String, StoredAudio> objects = new ConcurrentHashMap<>();

		@Override
		public String store(String objectKey, byte[] data, String contentType) {
			objects.put(objectKey, new StoredAudio(data.clone(), contentType));
			return "memory://" + objectKey;
		}

		@Override
		public Optional<StoredAudio> load(String objectKey) {
			return Optional.ofNullable(objects.get(objectKey));
		}

	}

}
