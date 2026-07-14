package com.kids.ai.service.gemini;

import java.util.List;

public record GeminiRequest(List<Content> contents) {

    public record Content(List<Part> parts) {}

    public record Part(String text) {}

    public static GeminiRequest of(String systemPrompt, String userMessage) {
        return new GeminiRequest(List.of(
            new Content(List.of(
                new Part("SYSTEM: " + systemPrompt + "\n\nПитання учня: " + userMessage)
            ))
        ));
    }
}
