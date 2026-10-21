package ua.kidlearn.themes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import ua.kidlearn.config.LlmProperties;

/**
 * AnthropicThemeGenerator against a local in-process HTTP server (no network, no real key): the
 * request carries nothing but the three catalog ids, the tool is strict and closed, and the
 * response is parsed (or refused) correctly.
 */
class AnthropicThemeGeneratorTest {

	private final ObjectMapper objectMapper = JsonMapper.builder().build();
	private HttpServer server;
	private final AtomicReference<String> lastBody = new AtomicReference<>();
	private volatile String responseBody;

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/v1/messages", exchange -> {
			lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("content-type", "application/json");
			exchange.sendResponseHeaders(200, bytes.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(bytes);
			}
		});
		server.start();
	}

	@AfterEach
	void stopServer() {
		server.stop(0);
	}

	private AnthropicThemeGenerator generator() {
		return new AnthropicThemeGenerator(new LlmProperties(2, "anthropic", "test-key-not-a-real-secret",
				"claude-sonnet-5-5", "http://127.0.0.1:" + server.getAddress().getPort(), 16000, Duration.ofSeconds(5),
				"high"), objectMapper);
	}

	private String toolUseResponse() {
		GeneratedTheme stubTheme = new StubThemeGenerator().generate("blue", "space", "calm");
		var response = objectMapper.createObjectNode();
		response.put("model", "claude-sonnet-5-5");
		response.put("stop_reason", "tool_use");
		var block = response.putArray("content").addObject();
		block.put("type", "tool_use").put("name", "emit_theme");
		var input = block.putObject("input");
		input.put("name", "Синій космос");
		input.put("guideAccessory", "helmet");
		var palette = input.putObject("palette");
		stubTheme.palette().forEach(palette::put);
		return response.toString();
	}

	@Test
	void theRequestCarriesOnlyTheThreeCatalogIdsAndAStrictClosedTool() {
		responseBody = toolUseResponse();
		generator().generate("blue", "space", "calm");

		JsonNode body = objectMapper.readTree(lastBody.get());
		JsonNode messages = body.get("messages");
		assertThat(messages).hasSize(1);
		assertThat(messages.get(0).get("content").asText()).isEqualTo("COLOR: blue\nWORLD: space\nMOOD: calm");
		assertThat(body.get("system").asText()).isEqualTo(AnthropicThemeGenerator.SYSTEM_PROMPT);

		// Every string value in the request is a fixed instruction, schema word or one of the ids: no
		// names, ages, ids of children or any other user data can be in it.
		List<String> strings = new ArrayList<>();
		collectStrings(body, strings);
		String fixedParts = AnthropicThemeGenerator.SYSTEM_PROMPT + "COLOR: blue\nWORLD: space\nMOOD: calm"
				+ String.join(" ", ThemeCatalog.PALETTE_TOKENS) + String.join(" ", ThemeCatalog.GUIDE_ACCESSORIES);
		for (String s : strings) {
			assertThat(fixedParts.contains(s) || List.of("user", "object", "string", "auto", "emit_theme",
					"claude-sonnet-5-5", "^#[0-9a-fA-F]{6}$", "name", "guideAccessory", "palette").contains(s)
					|| s.startsWith("Submit the theme")).as("unexpected string in the request: " + s).isTrue();
		}
		assertThat(lastBody.get()).doesNotContainPattern("\\d{4}-\\d{2}-\\d{2}|[0-9a-f]{8}-[0-9a-f]{4}-");

		JsonNode tool = body.get("tools").get(0);
		assertThat(tool.get("strict").asBoolean()).isTrue();
		assertThat(tool.get("input_schema").get("additionalProperties").asBoolean()).isFalse();
		assertThat(tool.get("input_schema").get("properties").get("palette").get("required")).hasSize(15);
		assertThat(tool.get("input_schema").get("properties").get("guideAccessory").get("enum")).hasSize(6);
	}

	private static void collectStrings(JsonNode node, List<String> out) {
		if (node.isString()) {
			out.add(node.asText());
		}
		// Values only: the keys are the fixed request/schema structure.
		node.properties().forEach(e -> collectStrings(e.getValue(), out));
		if (node.isArray()) {
			node.forEach(child -> collectStrings(child, out));
		}
	}

	@Test
	void theToolCallBecomesATheme() {
		responseBody = toolUseResponse();
		GeneratedTheme theme = generator().generate("blue", "space", "calm");
		assertThat(theme.name()).isEqualTo("Синій космос");
		assertThat(theme.guideAccessory()).isEqualTo("helmet");
		assertThat(theme.palette()).containsOnlyKeys(ThemeCatalog.PALETTE_TOKENS);
		assertThat(theme.model()).isEqualTo("claude-sonnet-5-5");
	}

	@Test
	void aRefusalOrAMissingToolCallFails() {
		responseBody = "{\"stop_reason\":\"refusal\",\"content\":[]}";
		assertThatThrownBy(() -> generator().generate("red", "sea", "bright"))
				.isInstanceOf(ThemeGenerationException.class);
		responseBody = "{\"stop_reason\":\"end_turn\",\"content\":[{\"type\":\"text\",\"text\":\"hi\"}]}";
		assertThatThrownBy(() -> generator().generate("red", "sea", "bright"))
				.isInstanceOf(ThemeGenerationException.class)
				.hasMessageContaining("no emit_theme tool call");
	}

}
