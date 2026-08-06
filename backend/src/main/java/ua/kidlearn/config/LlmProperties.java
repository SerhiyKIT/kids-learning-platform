package ua.kidlearn.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.llm")
public record LlmProperties(int maxAttempts) {
}
