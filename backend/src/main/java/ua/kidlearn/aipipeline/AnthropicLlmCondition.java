package ua.kidlearn.aipipeline;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * Picks exactly one LlmProvider bean: AnthropicLlmProvider when app.llm.provider=anthropic AND
 * app.llm.api-key is non-blank, StubLlmProvider otherwise (see {@link Inverse}). A plain
 * {@code @ConditionalOnProperty} can't express "non-blank", and an empty KIDLEARN_LLM_API_KEY
 * must fall back to the stub, never to a real provider that fails on its first call. Public: the
 * theme generator (ua.kidlearn.themes) is gated by exactly the same switch.
 */
public class AnthropicLlmCondition implements Condition {

	static final String PROVIDER_NAME = "anthropic";

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		return isEnabled(context.getEnvironment());
	}

	public static boolean isEnabled(Environment environment) {
		return PROVIDER_NAME.equalsIgnoreCase(environment.getProperty("app.llm.provider", "stub").strip())
				&& StringUtils.hasText(environment.getProperty("app.llm.api-key"));
	}

	/** Exact complement, so the context always holds one and only one LlmProvider. */
	public static class Inverse implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return !isEnabled(context.getEnvironment());
		}

	}

}
