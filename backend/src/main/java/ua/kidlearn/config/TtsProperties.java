package ua.kidlearn.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.tts")
public record TtsProperties(String voiceId, String storage) {
}
