package ua.kidlearn.scenario;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;

/**
 * The top-level scenario vocabulary (topic / age_band enums, pattern_ids shape), read straight out
 * of docs/lesson-scenario.schema.json so request validation elsewhere (e.g. the AI generation
 * request) never keeps a second, drifting copy of those lists.
 */
public record ScenarioVocabulary(Set<String> topics, Set<String> ageBands, Pattern patternId, int minPatternIds,
		int maxPatternIds) {

	static ScenarioVocabulary fromSchema(JsonNode schema) {
		JsonNode properties = schema.path("properties");
		JsonNode patternIds = properties.path("pattern_ids");
		String patternRegex = patternIds.path("items").path("pattern").asText(null);
		if (patternRegex == null || !patternIds.has("minItems") || !patternIds.has("maxItems")) {
			throw new IllegalStateException("lesson-scenario schema: pattern_ids items.pattern/minItems/maxItems missing");
		}
		return new ScenarioVocabulary(enumValues(properties, "topic"), enumValues(properties, "age_band"),
				Pattern.compile(patternRegex), patternIds.path("minItems").asInt(), patternIds.path("maxItems").asInt());
	}

	private static Set<String> enumValues(JsonNode properties, String name) {
		JsonNode values = properties.path(name).path("enum");
		if (!values.isArray() || values.isEmpty()) {
			throw new IllegalStateException("lesson-scenario schema: properties." + name + ".enum missing");
		}
		Set<String> result = new LinkedHashSet<>();
		values.forEach(v -> result.add(v.asText()));
		return Set.copyOf(result);
	}

}
