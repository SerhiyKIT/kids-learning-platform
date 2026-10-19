package ua.kidlearn.aipipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Derives the {@code input_schema} for a strict ({@code strict: true}) Anthropic tool from
 * docs/lesson-scenario.schema.json. Strict tool use accepts only a JSON Schema subset
 * (https://platform.claude.com/docs/en/build-with-claude/structured-outputs, "JSON Schema
 * limitations") and answers 400 to anything else, so this does what the official SDKs do for
 * their own schemas: drop the unsupported constraints rather than send them.
 *
 * <p>Nothing is lost for validation: the full, unmodified schema is still enforced afterwards by
 * ScenarioValidator (plus the business rules), and is also included verbatim in the user message
 * as JSON_SCHEMA so the model sees every constraint.
 */
final class StrictToolSchema {

	/** Numeric/string/array constraints strict mode rejects. */
	private static final Set<String> UNSUPPORTED_KEYWORDS = Set.of("minLength", "maxLength", "maxItems", "minimum",
			"maximum", "exclusiveMinimum", "exclusiveMaximum", "multipleOf");

	/** Annotations with no effect on validation that the strict-mode docs don't list as supported. */
	private static final Set<String> DROPPED_ANNOTATIONS = Set.of("$schema", "$id", "title");

	private static final String LOCAL_DEF_PREFIX = "#/$defs/";

	private StrictToolSchema() {
	}

	static ObjectNode from(JsonNode canonicalSchema) {
		ObjectNode schema = (ObjectNode) canonicalSchema.deepCopy();
		inlineAllOfBases(schema);
		strip(schema);
		return schema;
	}

	/**
	 * Strict mode doesn't support {@code allOf} combined with {@code $ref}. The scenario schema
	 * uses it only for "scene = sceneBase + own fields", with {@code true} placeholders for the
	 * inherited properties, so merge the base's properties/required in place of the placeholders
	 * and drop the now-unreferenced base definitions.
	 */
	private static void inlineAllOfBases(ObjectNode schema) {
		JsonNode defs = schema.get("$defs");
		if (!(defs instanceof ObjectNode defsNode)) {
			return;
		}
		List<String> inlinedBases = new ArrayList<>();
		for (Map.Entry<String, JsonNode> def : defsNode.properties()) {
			if (!(def.getValue() instanceof ObjectNode defNode) || !(defNode.get("allOf") instanceof ArrayNode allOf)) {
				continue;
			}
			for (JsonNode part : allOf) {
				String baseName = localDefName(part.path("$ref").asText(""));
				JsonNode base = defsNode.get(baseName);
				if (part.size() != 1 || base == null) {
					throw new IllegalStateException("Unsupported allOf entry in $defs/" + def.getKey() + ": " + part);
				}
				ObjectNode properties = defNode.withObjectProperty("properties");
				for (Map.Entry<String, JsonNode> baseProperty : base.path("properties").properties()) {
					JsonNode own = properties.get(baseProperty.getKey());
					if (own == null || own.isBoolean()) {
						properties.set(baseProperty.getKey(), baseProperty.getValue().deepCopy());
					}
				}
				ArrayNode required = defNode.withArrayProperty("required");
				for (JsonNode requiredName : base.path("required")) {
					if (!containsText(required, requiredName.asText())) {
						required.add(requiredName.asText());
					}
				}
				inlinedBases.add(baseName);
			}
			defNode.remove("allOf");
		}
		for (String baseName : inlinedBases) {
			if (!referencesDef(schema, LOCAL_DEF_PREFIX + baseName)) {
				defsNode.remove(baseName);
			}
		}
	}

	/** Keywords whose value is a name -> sub-schema map, not a schema itself. */
	private static final Set<String> SCHEMA_MAP_KEYWORDS = Set.of("properties", "$defs");

	private static void strip(JsonNode node) {
		if (node instanceof ObjectNode object) {
			object.remove(UNSUPPORTED_KEYWORDS);
			object.remove(DROPPED_ANNOTATIONS);
			// Only minItems 0 and 1 are supported; ScenarioValidator still enforces the real minimum.
			if (object.path("minItems").asInt(0) > 1) {
				object.put("minItems", 1);
			}
			// The scene variants are disjoint by their "type" const, so anyOf accepts exactly what
			// oneOf did — and anyOf is the union keyword strict mode supports.
			JsonNode oneOf = object.remove("oneOf");
			if (oneOf != null) {
				object.set("anyOf", oneOf);
			}
			if (object.has("properties") && !object.path("additionalProperties").isBoolean()) {
				object.put("additionalProperties", false);
			}
			for (Map.Entry<String, JsonNode> field : object.properties()) {
				if (SCHEMA_MAP_KEYWORDS.contains(field.getKey())) {
					// Strip each named sub-schema, never the map itself: "title" is both an
					// annotation keyword and a real scenario property name.
					for (Map.Entry<String, JsonNode> named : field.getValue().properties()) {
						if (named.getValue().isBoolean()) {
							throw new IllegalStateException("Boolean sub-schema left at '" + named.getKey() + "'");
						}
						strip(named.getValue());
					}
				} else {
					strip(field.getValue());
				}
			}
		} else if (node instanceof ArrayNode array) {
			for (JsonNode element : array) {
				strip(element);
			}
		}
	}

	private static boolean referencesDef(JsonNode node, String ref) {
		if (node.isObject()) {
			if (ref.equals(node.path("$ref").asText(null))) {
				return true;
			}
			for (Map.Entry<String, JsonNode> field : node.properties()) {
				if (referencesDef(field.getValue(), ref)) {
					return true;
				}
			}
		} else if (node.isArray()) {
			for (JsonNode element : node) {
				if (referencesDef(element, ref)) {
					return true;
				}
			}
		}
		return false;
	}

	private static String localDefName(String ref) {
		return ref.startsWith(LOCAL_DEF_PREFIX) ? ref.substring(LOCAL_DEF_PREFIX.length()) : "";
	}

	private static boolean containsText(ArrayNode array, String value) {
		for (JsonNode element : array) {
			if (value.equals(element.asText())) {
				return true;
			}
		}
		return false;
	}

}
