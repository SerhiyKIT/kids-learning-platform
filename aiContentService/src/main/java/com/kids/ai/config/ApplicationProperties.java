package com.kids.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to Ai Content Service.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 * See {@link tech.jhipster.config.JHipsterProperties} for a good example.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    private final Gemini gemini = new Gemini();

    public Gemini getGemini() {
        return gemini;
    }

    public static class Gemini {
        private String apiKey = "";
        private String model = "gemini-2.0-flash";
        private String promptStyle = "encouraging";

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getPromptStyle() { return promptStyle; }
        public void setPromptStyle(String promptStyle) { this.promptStyle = promptStyle; }
    }
}
