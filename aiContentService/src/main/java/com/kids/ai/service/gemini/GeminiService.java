package com.kids.ai.service.gemini;

import com.kids.ai.config.ApplicationProperties;
import com.kids.ai.domain.AiPrompt;
import com.kids.ai.repository.AiPromptRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GeminiService {

    private static final Logger LOG = LoggerFactory.getLogger(GeminiService.class);

    private static final String GEMINI_API_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private static final List<String> FALLBACK_HINTS = List.of(
        "Подумай уважно! 🤔 Пригадай, що ти вже знаєш про цю тему.",
        "Ти майже там! 💪 Спробуй ще раз — відповідь десь поряд.",
        "Не здавайся! 🌟 Іноді варто перечитати питання ще раз повільно.",
        "Цікаве питання! 🎯 Згадай схожі приклади з життя.",
        "Молодець, що намагаєшся! 🚀 Вір у себе — ти можеш!"
    );

    private final RestTemplate restTemplate;
    private final ApplicationProperties applicationProperties;
    private final AiPromptRepository aiPromptRepository;

    private int fallbackIndex = 0;

    public GeminiService(
        ApplicationProperties applicationProperties,
        AiPromptRepository aiPromptRepository
    ) {
        this.applicationProperties = applicationProperties;
        this.aiPromptRepository = aiPromptRepository;
        this.restTemplate = new RestTemplate();
    }

    public String generateHint(String questionText, String subject) {
        String apiKey = applicationProperties.getGemini().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            LOG.debug("Gemini API key not configured — returning fallback hint");
            return nextFallbackHint();
        }

        try {
            String style = applicationProperties.getGemini().getPromptStyle();
            String systemPrompt = aiPromptRepository.findByStyleName(style)
                .map(AiPrompt::getSystemPrompt)
                .orElse("Ти дружній вчитель для дітей. Дай підказку українською (1-2 речення), не давай прямої відповіді.");

            String userMessage = subject != null && !subject.isBlank()
                ? "[Предмет: " + subject + "] " + questionText
                : questionText;

            GeminiRequest request = GeminiRequest.of(systemPrompt, userMessage);
            String model = applicationProperties.getGemini().getModel();
            String url = String.format(GEMINI_API_URL, model, apiKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<GeminiRequest> entity = new HttpEntity<>(request, headers);

            GeminiResponse response = restTemplate.postForObject(url, entity, GeminiResponse.class);
            if (response != null) {
                String text = response.extractText();
                if (text != null && !text.isBlank()) {
                    return text.trim();
                }
            }
        } catch (Exception e) {
            LOG.warn("Gemini API call failed: {}", e.getMessage());
        }

        return nextFallbackHint();
    }

    private synchronized String nextFallbackHint() {
        String hint = FALLBACK_HINTS.get(fallbackIndex % FALLBACK_HINTS.size());
        fallbackIndex++;
        return hint;
    }
}
