package ua.kidlearn.aipipeline;

import java.util.Optional;

public interface AudioStorage {

	/** Stores the audio bytes under objectKey and returns a file_url for it. */
	String store(String objectKey, byte[] data, String contentType);

	/** Reads back what {@link #store} wrote under objectKey; empty if there's no such object. */
	Optional<StoredAudio> load(String objectKey);

}
