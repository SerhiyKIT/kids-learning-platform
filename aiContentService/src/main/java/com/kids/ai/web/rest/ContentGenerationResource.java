package com.kids.ai.web.rest;

import com.kids.ai.domain.GeneratedStory;
import com.kids.ai.repository.GeneratedStoryRepository;
import com.kids.ai.service.gemini.GeminiService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ContentGenerationResource {

    private static final Logger LOG = LoggerFactory.getLogger(ContentGenerationResource.class);

    private final GeminiService geminiService;
    private final GeneratedStoryRepository generatedStoryRepository;

    public ContentGenerationResource(GeminiService geminiService, GeneratedStoryRepository generatedStoryRepository) {
        this.geminiService = geminiService;
        this.generatedStoryRepository = generatedStoryRepository;
    }

    public record HintRequest(
        @NotBlank @Size(max = 500) String question,
        @Size(max = 100) String subject
    ) {}

    public record HintResponse(String hint) {}

    @PostMapping("/generate-hint")
    public ResponseEntity<HintResponse> generateHint(@Valid @RequestBody HintRequest request) {
        LOG.debug("REST request to generate hint for question: {}", request.question());

        String hint = geminiService.generateHint(request.question(), request.subject());

        // Persist in MongoDB for analytics / caching
        GeneratedStory story = new GeneratedStory();
        story.setTopic(request.subject() != null ? request.subject() : "general");
        story.setContent(hint);
        story.setCreatedAt(Instant.now());
        generatedStoryRepository.save(story);

        return ResponseEntity.ok(new HintResponse(hint));
    }
}
