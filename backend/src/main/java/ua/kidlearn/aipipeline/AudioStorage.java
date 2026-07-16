package ua.kidlearn.aipipeline;

public interface AudioStorage {

	/** Stores the audio bytes under objectKey and returns a file_url for it. */
	String store(String objectKey, byte[] data, String contentType);

}
