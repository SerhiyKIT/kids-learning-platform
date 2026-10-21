package ua.kidlearn.aipipeline;

/** Audio bytes read back from AudioStorage, with the content type they were stored under. */
public record StoredAudio(byte[] bytes, String contentType) {
}
