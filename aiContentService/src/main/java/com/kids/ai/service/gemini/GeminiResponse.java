package com.kids.ai.service.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiResponse(List<Candidate> candidates) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Candidate(Content content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(List<Part> parts) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Part(String text) {}

    public String extractText() {
        if (candidates == null || candidates.isEmpty()) return null;
        var content = candidates.get(0).content();
        if (content == null || content.parts() == null || content.parts().isEmpty()) return null;
        return content.parts().get(0).text();
    }
}
