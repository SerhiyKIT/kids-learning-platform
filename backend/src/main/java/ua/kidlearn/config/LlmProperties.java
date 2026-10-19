package ua.kidlearn.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * LLM generation settings. {@code provider}/{@code apiKey}/{@code model} come from the
 * KIDLEARN_LLM_* env vars (see application.yml and infra/.env.example); the key is never
 * committed and has no default. The real provider is only wired when provider=anthropic AND a
 * non-blank key is present — otherwise the deterministic stub stays active (see
 * ua.kidlearn.aipipeline.AnthropicLlmCondition).
 *
 * @param maxAttempts total generate() calls per lesson-version request before giving up
 * @param effort Messages API output_config.effort; set explicitly rather than relying on the
 * model's default
 */
@ConfigurationProperties(prefix = "app.llm")
public record LlmProperties(int maxAttempts, @DefaultValue("stub") String provider, String apiKey,
		@DefaultValue("claude-sonnet-5-5") String model, @DefaultValue("https://api.anthropic.com") String baseUrl,
		@DefaultValue("16000") int maxTokens, @DefaultValue("180s") Duration requestTimeout,
		@DefaultValue("high") String effort) {

	// Records get a generated toString() that would print apiKey; keep it out of any log line.
	@Override
	public String toString() {
		return "LlmProperties[maxAttempts=" + maxAttempts + ", provider=" + provider + ", apiKey="
				+ (apiKey == null || apiKey.isBlank() ? "<unset>" : "<redacted>") + ", model=" + model + ", baseUrl="
				+ baseUrl + ", maxTokens=" + maxTokens + ", requestTimeout=" + requestTimeout + ", effort=" + effort
				+ "]";
	}

}
