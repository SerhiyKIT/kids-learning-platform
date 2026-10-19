package ua.kidlearn.aipipeline;

import static org.assertj.core.api.Assertions.assertThat;

import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import ua.kidlearn.scenario.ScenarioFixtures;

/**
 * The strict tool schema must stay inside the subset Anthropic strict tool use accepts
 * (https://platform.claude.com/docs/en/build-with-claude/structured-outputs, "JSON Schema
 * limitations") while still accepting every scenario the canonical schema accepts.
 */
class StrictToolSchemaTest {

	private static final Set<String> FORBIDDEN_KEYWORDS = Set.of("oneOf", "allOf", "minLength", "maxLength",
			"maxItems", "minimum", "maximum", "exclusiveMinimum", "exclusiveMaximum", "multipleOf", "$schema", "$id");

	private final ObjectMapper objectMapper = JsonMapper.builder().build();

	private JsonNode canonicalSchema() throws IOException {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream("schema/lesson-scenario.schema.json")) {
			return objectMapper.readTree(in);
		}
	}

	@Test
	void containsOnlyStrictModeCompatibleKeywords() throws IOException {
		ObjectNode strict = StrictToolSchema.from(canonicalSchema());

		List<String> violations = new ArrayList<>();
		collectViolations(strict, "", violations);
		assertThat(violations).isEmpty();
		assertThat(strict.path("$defs").has("sceneBase")).isFalse();
		assertThat(strict.path("properties").has("title")).as("'title' the scenario property must survive").isTrue();
	}

	@Test
	void stillAcceptsTheCanonicalValidScenario() throws IOException {
		Schema strict = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_7)
				.getSchema(StrictToolSchema.from(canonicalSchema()));

		assertThat(strict.validate(objectMapper.readTree(ScenarioFixtures.VALID_MINIMAL_SCENARIO))).isEmpty();
	}

	@Test
	void stillRejectsUnknownSceneFieldsLikeTheCanonicalSchema() throws IOException {
		Schema strict = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_7)
				.getSchema(StrictToolSchema.from(canonicalSchema()));
		String withExtraField = ScenarioFixtures.VALID_MINIMAL_SCENARIO.replaceFirst("\"type\": \"demo\",",
				"\"type\": \"demo\", \"unexpected\": 1,");

		assertThat(strict.validate(objectMapper.readTree(withExtraField))).isNotEmpty();
	}

	@Test
	void doesNotModifyTheCanonicalSchema() throws IOException {
		JsonNode canonical = canonicalSchema();
		JsonNode before = canonical.deepCopy();

		StrictToolSchema.from(canonical);

		assertThat(canonical).isEqualTo(before);
	}

	private static void collectViolations(JsonNode schema, String path, List<String> violations) {
		if (schema.isBoolean()) {
			violations.add(path + ": boolean sub-schema");
			return;
		}
		if (!schema.isObject()) {
			return;
		}
		for (Map.Entry<String, JsonNode> field : schema.properties()) {
			String key = field.getKey();
			JsonNode value = field.getValue();
			String fieldPath = path + "/" + key;
			if (FORBIDDEN_KEYWORDS.contains(key)) {
				violations.add(fieldPath + ": unsupported keyword");
			}
			if ("minItems".equals(key) && value.asInt() > 1) {
				violations.add(fieldPath + ": minItems > 1");
			}
			if ("properties".equals(key) || "$defs".equals(key)) {
				for (Map.Entry<String, JsonNode> named : value.properties()) {
					collectViolations(named.getValue(), fieldPath + "/" + named.getKey(), violations);
				}
			} else if (value.isObject()) {
				collectViolations(value, fieldPath, violations);
			} else if (value.isArray()) {
				for (int i = 0; i < value.size(); i++) {
					collectViolations(value.get(i), fieldPath + "/" + i, violations);
				}
			}
		}
		if (schema.has("properties") && !schema.path("additionalProperties").isBoolean()) {
			violations.add(path + ": object without additionalProperties=false");
		}
		if (schema.has("properties") && schema.path("additionalProperties").asBoolean(false)) {
			violations.add(path + ": additionalProperties must be false");
		}
	}

}
