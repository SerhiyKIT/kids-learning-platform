package ua.kidlearn.aipipeline;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import ua.kidlearn.config.LlmProperties;
import ua.kidlearn.scenario.ScenarioProblem;

/**
 * Real lesson generator on the Anthropic Messages API (POST /v1/messages), over plain RestClient
 * rather than an SDK to keep dependencies minimal. Active only when app.llm.provider=anthropic and
 * an API key is set ({@link AnthropicLlmCondition}); otherwise StubLlmProvider is wired instead.
 *
 * <p>The scenario comes back as the input of a single strict tool call (emit_lesson_scenario), so
 * the API already guarantees it matches the tool schema. That is the first belt only: the result
 * still goes through ScenarioValidator (full schema + business rules) and human moderation exactly
 * like any other scenario — nothing here replaces either.
 *
 * <p>tool_choice is "auto", not forced: claude-sonnet-5-5 (and Opus 5.5 / Fable 5.1) answer 400 to
 * tool_choice "tool"/"any" (https://platform.claude.com/docs/en/agents-and-tools/tool-use/define-tools,
 * "Forcing tool use"). The prompt asks for the tool call and a response without one is rejected
 * as {@link LlmGenerationException.Reason#NO_SCENARIO}.
 *
 * <p>No sampling parameters are sent: the prompt doc suggests temperature ≈ 0.7, but current
 * models reject non-default temperature with a 400.
 */
@Component
@Conditional(AnthropicLlmCondition.class)
class AnthropicLlmProvider implements LlmProvider {

	private static final Logger log = LoggerFactory.getLogger(AnthropicLlmProvider.class);

	static final String TOOL_NAME = "emit_lesson_scenario";
	private static final String ANTHROPIC_VERSION = "2023-06-01";
	private static final String SCHEMA_CLASSPATH_LOCATION = "schema/lesson-scenario.schema.json";
	private static final String PROMPT_CLASSPATH_LOCATION = "prompt/Промпт_генерації_уроків.md";
	// The system prompt is the first fenced block after the "## 1." heading of the prompt doc.
	private static final Pattern SYSTEM_PROMPT_BLOCK = Pattern.compile("(?s)^## 1\\..*?^```[^\\n]*\\n(.*?)^```",
			Pattern.MULTILINE);
	private static final String TOOL_DESCRIPTION = """
			Submit the finished lesson scenario. Call this exactly once with the complete scenario \
			as the input object; it must follow the lesson scenario schema and every rule from the \
			system prompt. This is the only way to return the lesson — do not put the scenario in a \
			text reply.""";

	private final LlmProperties properties;
	private final ObjectMapper objectMapper;
	private final RestClient restClient;
	private final String systemPrompt;
	private final String canonicalSchemaText;
	private final ObjectNode toolInputSchema;

	AnthropicLlmProvider(LlmProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.canonicalSchemaText = readClasspathText(SCHEMA_CLASSPATH_LOCATION);
		this.toolInputSchema = StrictToolSchema.from(objectMapper.readTree(canonicalSchemaText));
		this.systemPrompt = extractSystemPrompt(readClasspathText(PROMPT_CLASSPATH_LOCATION));

		// Built here, not on first call, but construction still does no network I/O.
		HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.requestTimeout()).build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(properties.requestTimeout());
		this.restClient = RestClient.builder()
				.baseUrl(properties.baseUrl())
				.requestFactory(requestFactory)
				.defaultHeader("x-api-key", properties.apiKey())
				.defaultHeader("anthropic-version", ANTHROPIC_VERSION)
				.build();
	}

	@Override
	public GeneratedScenario generate(GenerationRequest request) {
		log.info("Requesting lesson scenario from Anthropic: model={}, topic={}, ageBand={}, retryWithFeedback={}",
				properties.model(), request.topic(), request.ageBand(), request.previousProblems() != null);
		String responseBody = post(objectMapper.writeValueAsString(buildRequestBody(request)));
		return parseResponse(responseBody);
	}

	ObjectNode buildRequestBody(GenerationRequest request) {
		ObjectNode body = objectMapper.createObjectNode();
		body.put("model", properties.model());
		body.put("max_tokens", properties.maxTokens());
		body.put("system", systemPrompt);
		body.withArrayProperty("messages").addObject().put("role", "user").put("content", buildUserMessage(request));

		ObjectNode tool = body.withArrayProperty("tools").addObject();
		tool.put("name", TOOL_NAME);
		tool.put("description", TOOL_DESCRIPTION);
		tool.set("input_schema", toolInputSchema);
		tool.put("strict", true);

		body.putObject("tool_choice").put("type", "auto").put("disable_parallel_tool_use", true);
		body.putObject("output_config").put("effort", properties.effort());
		return body;
	}

	/** User message in the docs/Промпт_генерації_уроків.md §2 layout. */
	String buildUserMessage(GenerationRequest request) {
		StringBuilder message = new StringBuilder("ЗАВДАННЯ: створи урок.\n");
		// TODO: send the full pattern text from docs/Вимоги_модуль_Безпека.md, not just its id —
		// GenerationRequest only carries ids today and there is no pattern catalog in code yet.
		message.append("PATTERN: ").append(String.join(", ", request.patternIds())).append('\n');
		message.append("AGE_BAND: ").append(request.ageBand()).append('\n');
		message.append("TOPIC: ").append(request.topic()).append('\n');
		if (request.title() != null && !request.title().isBlank()) {
			message.append("TITLE: ").append(request.title()).append('\n');
		}
		if (request.learningGoal() != null && !request.learningGoal().isBlank()) {
			message.append("LEARNING_GOAL: ").append(request.learningGoal()).append('\n');
		}
		// TODO: fill VERIFIED_FACTS and ASSET_REGISTRY from real sources once they exist in code
		// (facts table from docs/Вимоги_модуль_Безпека.md, the exported asset registry). Until then
		// the prompt's own rule applies: no fact outside VERIFIED_FACTS may be used.
		message.append("\nVERIFIED_FACTS:\n(немає — не використовуй жодних фактів поза цим блоком)\n");
		message.append("\nASSET_REGISTRY:\n(реєстр ще не підключено — використовуй короткі описові id у форматі assetRef)\n");
		message.append("\nJSON_SCHEMA:\n").append(canonicalSchemaText.strip()).append('\n');
		if (request.previousProblems() != null && !request.previousProblems().isEmpty()) {
			message.append("\nПОПЕРЕДНЯ СПРОБА НЕ ПРОЙШЛА ВАЛІДАЦІЮ. Виправ ці проблеми:\n");
			for (ScenarioProblem problem : request.previousProblems()) {
				message.append("- ").append(problem.path()).append(": ").append(problem.message()).append('\n');
			}
		}
		message.append("\nПоверни сценарій ЛИШЕ одним викликом інструмента ").append(TOOL_NAME).append('.');
		return message.toString();
	}

	private String post(String requestBody) {
		try {
			return restClient.post()
					.uri("/v1/messages")
					.contentType(MediaType.APPLICATION_JSON)
					.body(requestBody)
					.retrieve()
					.body(String.class);
		} catch (RestClientResponseException e) {
			// Status + Anthropic's error type only; the response body may echo request content.
			String errorType = errorType(e.getResponseBodyAsString());
			log.warn("Anthropic API returned {} ({})", e.getStatusCode().value(), errorType);
			throw new LlmGenerationException(LlmGenerationException.Reason.UPSTREAM_ERROR,
					"Anthropic API returned " + e.getStatusCode().value() + " (" + errorType + ")", e);
		} catch (ResourceAccessException e) {
			log.warn("Anthropic API unreachable or timed out: {}", e.getClass().getSimpleName());
			throw new LlmGenerationException(LlmGenerationException.Reason.UNAVAILABLE,
					"Anthropic API unreachable or timed out", e);
		} catch (RestClientException e) {
			log.warn("Anthropic API call failed: {}", e.getClass().getSimpleName());
			throw new LlmGenerationException(LlmGenerationException.Reason.UPSTREAM_ERROR, "Anthropic API call failed",
					e);
		}
	}

	GeneratedScenario parseResponse(String responseBody) {
		JsonNode response;
		try {
			response = objectMapper.readTree(responseBody == null ? "" : responseBody);
		} catch (JacksonException e) {
			throw new LlmGenerationException(LlmGenerationException.Reason.UPSTREAM_ERROR,
					"Anthropic API returned a non-JSON body", e);
		}
		String stopReason = response.path("stop_reason").asText("");
		log.info("Anthropic response: stop_reason={}, input_tokens={}, output_tokens={}", stopReason,
				response.path("usage").path("input_tokens").asInt(), response.path("usage").path("output_tokens").asInt());

		// Check stop_reason before reading content: a refused or truncated turn may hold a partial tool call.
		if ("refusal".equals(stopReason)) {
			String category = response.path("stop_details").path("category").asText("unknown");
			throw new LlmGenerationException(LlmGenerationException.Reason.REFUSED,
					"Model declined to generate the lesson (category: " + category + ")");
		}
		if ("max_tokens".equals(stopReason)) {
			throw new LlmGenerationException(LlmGenerationException.Reason.TRUNCATED,
					"Model output hit max_tokens=" + properties.maxTokens() + " before the scenario was complete");
		}
		JsonNode content = response.path("content");
		if (content instanceof ArrayNode blocks) {
			for (JsonNode block : blocks) {
				if ("tool_use".equals(block.path("type").asText()) && TOOL_NAME.equals(block.path("name").asText())
						&& block.path("input").isObject()) {
					return new GeneratedScenario(block.get("input"), response.path("model").asText(properties.model()));
				}
			}
		}
		throw new LlmGenerationException(LlmGenerationException.Reason.NO_SCENARIO,
				"Model response (stop_reason=" + stopReason + ") contained no " + TOOL_NAME + " tool call");
	}

	private String errorType(String errorBody) {
		try {
			return objectMapper.readTree(errorBody).path("error").path("type").asText("unknown");
		} catch (JacksonException e) {
			return "unknown";
		}
	}

	static String extractSystemPrompt(String promptDoc) {
		Matcher matcher = SYSTEM_PROMPT_BLOCK.matcher(promptDoc);
		if (!matcher.find() || matcher.group(1).isBlank()) {
			throw new IllegalStateException(PROMPT_CLASSPATH_LOCATION + ": no system prompt code block under '## 1.'");
		}
		return matcher.group(1).strip();
	}

	private String readClasspathText(String location) {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(location)) {
			if (in == null) {
				throw new IllegalStateException(location + " missing from classpath");
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
