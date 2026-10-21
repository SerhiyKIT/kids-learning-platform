package ua.kidlearn.themes;

import java.net.http.HttpClient;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import ua.kidlearn.aipipeline.AnthropicLlmCondition;
import ua.kidlearn.config.LlmProperties;

/**
 * Theme generator on the Anthropic Messages API, behind the same switch as the lesson LLM
 * (app.llm.provider=anthropic + a non-blank key; {@link AnthropicLlmCondition}). The answer comes
 * back as the input of one strict tool call (emit_theme) whose schema only allows the catalog's
 * tokens as #RRGGBB strings and the catalog's accessories — and it still goes through
 * ThemeValidator and human moderation afterwards.
 *
 * <p>The user message carries ONLY the three catalog ids: no name, age or anything else about a
 * child ever reaches the model. The model picks colours — it never draws anything.
 */
@Component
@Conditional(AnthropicLlmCondition.class)
class AnthropicThemeGenerator implements ThemeGenerator {

	private static final Logger log = LoggerFactory.getLogger(AnthropicThemeGenerator.class);

	static final String TOOL_NAME = "emit_theme";
	private static final String ANTHROPIC_VERSION = "2023-06-01";
	private static final int MAX_TOKENS = 4000;

	static final String SYSTEM_PROMPT = """
			Ти підбираєш колірну тему оформлення для дитячого навчального застосунку (діти 3–6 років).
			Тема — це ЛИШЕ значення готових колірних токенів, аксесуар провідника-зайченяти з переліку та \
			коротка назва для модератора. Ти нічого не малюєш і не описуєш зображень.

			Токени (усі обов'язкові, формат #RRGGBB):
			- bg-top, canvas, bg-bottom — фон сцени згори донизу: світлі, спокійні (HSL L ≥ 0.80);
			- ink — текст і контури: контраст з кожним фоном ≥ 4.5:1;
			- ink-soft — другорядні контури;
			- sun — головна кнопка «послухати»; teal — кнопки «далі» та альбом: контраст з кожним фоном ≥ 3:1;
			- sun-deep — іконка на кнопці sun: контраст із sun ≥ 3:1;
			- pip, glow, scroll — крапки прогресу, підсвітка, смуга прокрутки;
			- correct, correct-surface — лагідний «правильно» та його світле тло;
			- guide-main, guide-accent — шерсть і вушка провідника.

			Правила: жодних «кислотних» неонових кольорів (хрома HSL (1−|2L−1|)·S ≤ 0.82); теплі, дружні \
			поєднання в межах норм і моралі; спокійно (calm) — приглушені, яскраво (bright) — насиченіші, але не \
			кислотні. Колір дитини має бути впізнаваним у фоні та кнопках, світ — підказка для аксесуара.
			Відповідай ЛИШЕ одним викликом інструмента emit_theme.""";

	private static final String TOOL_DESCRIPTION = """
			Submit the theme: the palette (every token as #RRGGBB), one guide accessory from the list, \
			and a short Ukrainian name for the moderator. Call exactly once.""";

	private final LlmProperties properties;
	private final ObjectMapper objectMapper;
	private final RestClient restClient;

	AnthropicThemeGenerator(LlmProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
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
	public GeneratedTheme generate(String colorId, String worldId, String moodId) {
		log.info("Requesting theme from Anthropic: model={}, preferenceKey={}", properties.model(),
				ThemeCatalog.preferenceKey(colorId, worldId, moodId));
		String responseBody;
		try {
			responseBody = restClient.post()
					.uri("/v1/messages")
					.contentType(MediaType.APPLICATION_JSON)
					.body(objectMapper.writeValueAsString(buildRequestBody(colorId, worldId, moodId)))
					.retrieve()
					.body(String.class);
		} catch (RestClientResponseException e) {
			log.warn("Anthropic API returned {} for a theme", e.getStatusCode().value());
			throw new ThemeGenerationException("Anthropic API returned " + e.getStatusCode().value(), e);
		} catch (RestClientException e) {
			log.warn("Anthropic API call for a theme failed: {}", e.getClass().getSimpleName());
			throw new ThemeGenerationException("Anthropic API unreachable or failed", e);
		}
		return parseResponse(responseBody);
	}

	ObjectNode buildRequestBody(String colorId, String worldId, String moodId) {
		ObjectNode body = objectMapper.createObjectNode();
		body.put("model", properties.model());
		body.put("max_tokens", MAX_TOKENS);
		body.put("system", SYSTEM_PROMPT);
		body.withArrayProperty("messages").addObject().put("role", "user")
				.put("content", userMessage(colorId, worldId, moodId));
		ObjectNode tool = body.withArrayProperty("tools").addObject();
		tool.put("name", TOOL_NAME);
		tool.put("description", TOOL_DESCRIPTION);
		tool.set("input_schema", toolSchema());
		tool.put("strict", true);
		// "auto", not forced: current models answer 400 to a forced tool_choice (see AnthropicLlmProvider).
		body.putObject("tool_choice").put("type", "auto").put("disable_parallel_tool_use", true);
		return body;
	}

	/** Only the three catalog ids — the whole input about "the child". */
	static String userMessage(String colorId, String worldId, String moodId) {
		return "COLOR: " + colorId + "\nWORLD: " + worldId + "\nMOOD: " + moodId;
	}

	private ObjectNode toolSchema() {
		ObjectNode schema = objectMapper.createObjectNode();
		schema.put("type", "object");
		schema.put("additionalProperties", false);
		ObjectNode props = schema.putObject("properties");
		props.putObject("name").put("type", "string");
		ArrayNode accessories = props.putObject("guideAccessory").put("type", "string").putArray("enum");
		ThemeCatalog.GUIDE_ACCESSORIES.forEach(accessories::add);
		ObjectNode palette = props.putObject("palette");
		palette.put("type", "object");
		palette.put("additionalProperties", false);
		ObjectNode tokens = palette.putObject("properties");
		ArrayNode requiredTokens = palette.putArray("required");
		for (String token : ThemeCatalog.PALETTE_TOKENS) {
			tokens.putObject(token).put("type", "string").put("pattern", "^#[0-9a-fA-F]{6}$");
			requiredTokens.add(token);
		}
		schema.putArray("required").add("name").add("guideAccessory").add("palette");
		return schema;
	}

	GeneratedTheme parseResponse(String responseBody) {
		JsonNode response;
		try {
			response = objectMapper.readTree(responseBody == null ? "" : responseBody);
		} catch (JacksonException e) {
			throw new ThemeGenerationException("Anthropic API returned a non-JSON body", e);
		}
		String stopReason = response.path("stop_reason").asText("");
		if ("refusal".equals(stopReason)) {
			throw new ThemeGenerationException("Model declined to generate the theme");
		}
		if ("max_tokens".equals(stopReason)) {
			throw new ThemeGenerationException("Model output hit max_tokens before the theme was complete");
		}
		if (response.path("content") instanceof ArrayNode blocks) {
			for (JsonNode block : blocks) {
				if ("tool_use".equals(block.path("type").asText()) && TOOL_NAME.equals(block.path("name").asText())
						&& block.path("input").isObject()) {
					JsonNode input = block.get("input");
					Map<String, String> palette = new LinkedHashMap<>();
					input.path("palette").properties().forEach(e -> palette.put(e.getKey(), e.getValue().asText()));
					return new GeneratedTheme(input.path("name").asText(""), palette,
							input.path("guideAccessory").asText(""), response.path("model").asText(properties.model()));
				}
			}
		}
		throw new ThemeGenerationException("Model response (stop_reason=" + stopReason + ") contained no " + TOOL_NAME
				+ " tool call");
	}

}
