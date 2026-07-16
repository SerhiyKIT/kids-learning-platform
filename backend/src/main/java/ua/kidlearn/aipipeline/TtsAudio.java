package ua.kidlearn.aipipeline;

public record TtsAudio(byte[] bytes, String contentType, int durationMs) {
}
