package ua.kidlearn.aipipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import ua.kidlearn.config.LlmProperties;
import ua.kidlearn.scenario.ScenarioFixtures;
import ua.kidlearn.scenario.ScenarioProblem;
import ua.kidlearn.scenario.ScenarioValidator;

/**
 * AnthropicLlmProvider against a local in-process HTTP server (JDK HttpServer — no network, no
 * real key, no extra test dependency), so the real RestClient + timeout wiring is exercised.
 */
class AnthropicLlmProviderTest {

	private static final String FAKE_KEY = "test-key-not-a-real-secret";

	private static final GenerationRequest REQUEST = new GenerationRequest("road_safety", List.of("1.6"), "age_5_6",
			"Crossing the street", "Look both ways before crossing", null);

	private final ObjectMapper objectMapper = JsonMapper.builder().build();
	private final ScenarioValidator scenarioValidator = new ScenarioValidator(objectMapper);

	private HttpServer server;
	private final AtomicReference<RecordedRequest> lastRequest = new AtomicReference<>();
	private volatile int responseStatus;
	private volatile String responseBody;
	private volatile CountDownLatch blockResponseUntil;

	private record RecordedRequest(String method, String path, String apiKey, String version, String contentType,
			String body) {
	}

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", this::handle);
		server.start();
	}

	@AfterEach
	void stopServer() {
		if (blockResponseUntil != null) {
			blockResponseUntil.countDown();
		}
		server.stop(0);
	}

	private void handle(HttpExchange exchange) throws IOException {
		String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
		lastRequest.set(new RecordedRequest(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
				exchange.getRequestHeaders().getFirst("x-api-key"),
				exchange.getRequestHeaders().getFirst("anthropic-version"),
				exchange.getRequestHeaders().getFirst("content-type"), body));
		if (blockResponseUntil != null) {
			try {
				blockResponseUntil.await(10, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("content-type", "application/json");
		exchange.sendResponseHeaders(responseStatus, bytes.length);
		try (OutputStream out = exchange.getResponseBody()) {
			out.write(bytes);
		}
	}

	private AnthropicLlmProvider provider(Duration timeout) {
		LlmProperties properties = new LlmProperties(2, "anthropic", FAKE_KEY, "claude-sonnet-5-5",
				"http://127.0.0.1:" + server.getAddress().getPort(), 16000, timeout, "high");
		return new AnthropicLlmProvider(properties, objectMapper);
	}

	private AnthropicLlmProvider provider() {
		return provider(Duration.ofSeconds(5));
	}

	private void respond(int status, String body) {
		this.responseStatus = status;
		this.responseBody = body;
	}

	/** Canonical Messages API response: an (omitted-display) thinking block, then the strict tool call. */
	private static String toolUseResponse(String scenarioJson) {
		return """
				{"id":"msg_01","type":"message","role":"assistant","model":"claude-sonnet-5-5",
				 "content":[
				   {"type":"thinking","thinking":"","signature":"sig"},
				   {"type":"tool_use","id":"toolu_01","name":"emit_lesson_scenario","input":%s}
				 ],
				 "stop_reason":"tool_use","stop_details":null,
				 "usage":{"input_tokens":5200,"output_tokens":1400}}
				""".formatted(scenarioJson);
	}

	@Test
	void toolUseResponseIsParsedIntoScenarioThatPassesScenarioValidator() {
		respond(200, toolUseResponse(ScenarioFixtures.VALID_MINIMAL_SCENARIO));

		GeneratedScenario generated = provider().generate(REQUEST);

		assertThat(generated.modelId()).isEqualTo("claude-sonnet-5-5");
		List<ScenarioProblem> problems = scenarioValidator.validate(generated.scenario());
		assertThat(problems).isEmpty();
		assertThat(generated.scenario()).isEqualTo(objectMapper.readTree(ScenarioFixtures.VALID_MINIMAL_SCENARIO));
	}

	@Test
	void requestHasMessagesApiShapeHeadersAndStrictTool() {
		respond(200, toolUseResponse(ScenarioFixtures.VALID_MINIMAL_SCENARIO));

		provider().generate(REQUEST);

		RecordedRequest recorded = lastRequest.get();
		assertThat(recorded.method()).isEqualTo("POST");
		assertThat(recorded.path()).isEqualTo("/v1/messages");
		assertThat(recorded.apiKey()).isEqualTo(FAKE_KEY);
		assertThat(recorded.version()).isEqualTo("2023-06-01");
		assertThat(recorded.contentType()).startsWith("application/json");

		JsonNode body = objectMapper.readTree(recorded.body());
		assertThat(body.path("model").asText()).isEqualTo("claude-sonnet-5-5");
		assertThat(body.path("max_tokens").asInt()).isEqualTo(16000);
		assertThat(body.path("system").asText()).startsWith("Ти — сценарист навчальних уроків")
				.contains("Контрольна сцена — остання інтерактивна, is_control=true.");
		assertThat(body.has("temperature")).isFalse();
		assertThat(body.path("output_config").path("effort").asText()).isEqualTo("high");

		JsonNode tool = body.path("tools").get(0);
		assertThat(body.path("tools").size()).isEqualTo(1);
		assertThat(tool.path("name").asText()).isEqualTo("emit_lesson_scenario");
		assertThat(tool.path("strict").asBoolean()).isTrue();
		assertThat(tool.path("input_schema").path("type").asText()).isEqualTo("object");
		assertThat(tool.path("input_schema").path("required").toString()).contains("\"scenes\"");

		JsonNode toolChoice = body.path("tool_choice");
		assertThat(toolChoice.path("type").asText()).isEqualTo("auto");
		assertThat(toolChoice.path("disable_parallel_tool_use").asBoolean()).isTrue();

		JsonNode message = body.path("messages").get(0);
		assertThat(message.path("role").asText()).isEqualTo("user");
		assertThat(message.path("content").asText()).contains("ЗАВДАННЯ: створи урок.", "PATTERN: 1.6",
				"AGE_BAND: age_5_6", "TOPIC: road_safety", "VERIFIED_FACTS:", "ASSET_REGISTRY:", "JSON_SCHEMA:",
				"\"$defs\"", "emit_lesson_scenario");
	}

	@Test
	void previousProblemsAreFedBackIntoTheUserMessage() {
		respond(200, toolUseResponse(ScenarioFixtures.VALID_MINIMAL_SCENARIO));

		provider().generate(REQUEST.withPreviousProblems(List.of(new ScenarioProblem("/scenes/2/is_control",
				"Остання інтерактивна сцена має бути контрольною (is_control=true)"))));

		String content = objectMapper.readTree(lastRequest.get().body()).path("messages").get(0).path("content").asText();
		assertThat(content).contains("ПОПЕРЕДНЯ СПРОБА", "/scenes/2/is_control: Остання інтерактивна сцена");
	}

	@Test
	void non2xxBecomesUpstreamErrorWithoutLeakingTheKey() {
		respond(401, """
				{"type":"error","error":{"type":"authentication_error","message":"invalid x-api-key"}}
				""");

		assertThatThrownBy(() -> provider().generate(REQUEST)).isInstanceOfSatisfying(LlmGenerationException.class,
				e -> {
					assertThat(e.reason()).isEqualTo(LlmGenerationException.Reason.UPSTREAM_ERROR);
					assertThat(e.getMessage()).contains("401", "authentication_error").doesNotContain(FAKE_KEY);
				});
	}

	@Test
	void serverErrorBecomesUpstreamError() {
		respond(529, """
				{"type":"error","error":{"type":"overloaded_error","message":"Overloaded"}}
				""");

		assertThatThrownBy(() -> provider().generate(REQUEST)).isInstanceOfSatisfying(LlmGenerationException.class,
				e -> assertThat(e.reason()).isEqualTo(LlmGenerationException.Reason.UPSTREAM_ERROR));
	}

	@Test
	void textOnlyResponseWithoutToolUseBecomesNoScenario() {
		respond(200, """
				{"id":"msg_02","type":"message","role":"assistant","model":"claude-sonnet-5-5",
				 "content":[{"type":"text","text":"Ось ваш урок: ..."}],
				 "stop_reason":"end_turn","stop_details":null,"usage":{"input_tokens":10,"output_tokens":5}}
				""");

		assertThatThrownBy(() -> provider().generate(REQUEST)).isInstanceOfSatisfying(LlmGenerationException.class,
				e -> assertThat(e.reason()).isEqualTo(LlmGenerationException.Reason.NO_SCENARIO));
	}

	@Test
	void refusalBecomesRefusedEvenIfAPartialToolCallIsPresent() {
		respond(200, """
				{"id":"msg_03","type":"message","role":"assistant","model":"claude-sonnet-5-5",
				 "content":[{"type":"tool_use","id":"toolu_02","name":"emit_lesson_scenario","input":{}}],
				 "stop_reason":"refusal",
				 "stop_details":{"type":"refusal","category":"general_harms","explanation":"..."},
				 "usage":{"input_tokens":10,"output_tokens":5}}
				""");

		assertThatThrownBy(() -> provider().generate(REQUEST)).isInstanceOfSatisfying(LlmGenerationException.class,
				e -> {
					assertThat(e.reason()).isEqualTo(LlmGenerationException.Reason.REFUSED);
					assertThat(e.getMessage()).contains("general_harms");
				});
	}

	@Test
	void maxTokensBecomesTruncated() {
		respond(200, """
				{"id":"msg_04","type":"message","role":"assistant","model":"claude-sonnet-5-5",
				 "content":[{"type":"tool_use","id":"toolu_03","name":"emit_lesson_scenario","input":{"schema_version":"1.0.0"}}],
				 "stop_reason":"max_tokens","stop_details":null,"usage":{"input_tokens":10,"output_tokens":16000}}
				""");

		assertThatThrownBy(() -> provider().generate(REQUEST)).isInstanceOfSatisfying(LlmGenerationException.class,
				e -> assertThat(e.reason()).isEqualTo(LlmGenerationException.Reason.TRUNCATED));
	}

	@Test
	void slowResponseBecomesUnavailableAfterRequestTimeout() {
		respond(200, toolUseResponse(ScenarioFixtures.VALID_MINIMAL_SCENARIO));
		blockResponseUntil = new CountDownLatch(1); // released in @AfterEach

		assertThatThrownBy(() -> provider(Duration.ofMillis(300)).generate(REQUEST))
				.isInstanceOfSatisfying(LlmGenerationException.class,
						e -> assertThat(e.reason()).isEqualTo(LlmGenerationException.Reason.UNAVAILABLE));
	}

	@Test
	void systemPromptIsTheSection1CodeBlockOfThePromptDoc() {
		String doc = """
				# Title

				## 1. Системний промпт

				```
				Line one
				Line two
				```

				## 2. Інше

				```
				not this
				```
				""";
		assertThat(AnthropicLlmProvider.extractSystemPrompt(doc)).isEqualTo("Line one\nLine two");
		assertThatThrownBy(() -> AnthropicLlmProvider.extractSystemPrompt("# no section 1"))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void apiKeyNeverAppearsInPropertiesToString() {
		LlmProperties properties = new LlmProperties(2, "anthropic", FAKE_KEY, "claude-sonnet-5-5",
				"https://api.anthropic.com", 16000, Duration.ofSeconds(1), "high");
		assertThat(properties.toString()).doesNotContain(FAKE_KEY).contains("<redacted>");
	}

}
