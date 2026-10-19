package ua.kidlearn.aipipeline;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * Picks exactly one TtsProvider bean: GoogleTtsProvider when app.tts.provider=google AND
 * app.tts.api-key is non-blank, StubTtsProvider otherwise (see {@link Inverse}). Same shape as
 * {@link AnthropicLlmCondition}: an empty KIDLEARN_TTS_API_KEY must fall back to the stub, never
 * to a real provider that fails on its first call.
 */
class GoogleTtsCondition implements Condition {

	static final String PROVIDER_NAME = "google";

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		return isEnabled(context.getEnvironment());
	}

	static boolean isEnabled(Environment environment) {
		return PROVIDER_NAME.equalsIgnoreCase(environment.getProperty("app.tts.provider", "stub").strip())
				&& StringUtils.hasText(environment.getProperty("app.tts.api-key"));
	}

	/** Exact complement, so the context always holds one and only one TtsProvider. */
	static class Inverse implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return !isEnabled(context.getEnvironment());
		}

	}

}
