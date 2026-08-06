package ua.kidlearn.scenario;

import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Validates a lesson scenario against docs/lesson-scenario.schema.json (structural shape) plus
 * the business rules the schema can't express (see the schema file's own field descriptions and
 * docs/Формат_подання_уроків_та_ШІ.md §4.2). Standalone so both the admin content-entry endpoint
 * and the future LLM generation pipeline can reuse it.
 */
@Service
public class ScenarioValidator {

	private static final String SCHEMA_CLASSPATH_LOCATION = "schema/lesson-scenario.schema.json";
	private static final int TIER_A_MAX_WORDS = 10;
	private static final int TIER_B_MAX_WORDS = 15;
	private static final int TIER_B_MIN_SCENES = 5;

	private final Schema schema;

	public ScenarioValidator(ObjectMapper objectMapper) {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(SCHEMA_CLASSPATH_LOCATION)) {
			if (in == null) {
				throw new IllegalStateException(SCHEMA_CLASSPATH_LOCATION + " missing from classpath");
			}
			JsonNode schemaNode = objectMapper.readTree(in);
			// The doc's "$id" is a bare filename, not a resolvable IRI, and the validator library
			// requires one when present. All $refs in the schema are local ("#/$defs/..."), which
			// resolve fine without it, so drop it rather than touch the canonical doc.
			if (schemaNode instanceof ObjectNode objectNode) {
				schemaNode = objectNode.without("$id");
			}
			this.schema = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_7).getSchema(schemaNode);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** Validates using the tier word limit implied by the scenario's own scene count (see the schema doc). */
	public List<ScenarioProblem> validate(JsonNode scenario) {
		return validate(scenario, defaultMaxWordsPerVoiceLine(scenario));
	}

	/** Validates with an explicit voice-line word-count ceiling, overriding the scene-count-derived default. */
	public List<ScenarioProblem> validate(JsonNode scenario, int maxWordsPerVoiceLine) {
		List<ScenarioProblem> problems = new ArrayList<>();
		for (com.networknt.schema.Error error : schema.validate(scenario)) {
			problems.add(new ScenarioProblem(error.getInstanceLocation().toString(), error.getMessage()));
		}
		// Business rules assume a structurally sound tree; skip them if the schema already rejected it.
		if (problems.isEmpty()) {
			checkVoiceLines(scenario, "", new HashMap<>(), maxWordsPerVoiceLine, problems);
			checkScenes(scenario.path("scenes"), problems);
		}
		return problems;
	}

	/** Level A (3–4 scenes) allows ≤10 words per voice line; level B (5–6 scenes) allows ≤15. */
	private static int defaultMaxWordsPerVoiceLine(JsonNode scenario) {
		JsonNode scenes = scenario.path("scenes");
		return scenes.isArray() && scenes.size() >= TIER_B_MIN_SCENES ? TIER_B_MAX_WORDS : TIER_A_MAX_WORDS;
	}

	/** Recursively finds every {key,text} voiceLine object, checking key uniqueness and word count. */
	private void checkVoiceLines(JsonNode node, String path, Map<String, String> firstPathByKey, int maxWords,
			List<ScenarioProblem> problems) {
		if (node.isObject()) {
			JsonNode keyNode = node.get("key");
			JsonNode textNode = node.get("text");
			if (keyNode != null && keyNode.isTextual() && textNode != null && textNode.isTextual()) {
				String key = keyNode.asText();
				String firstSeenAt = firstPathByKey.putIfAbsent(key, path);
				if (firstSeenAt != null) {
					problems.add(new ScenarioProblem(path + "/key",
							"Ключ репліки '" + key + "' повторюється (вперше зустрічається у " + firstSeenAt + "/key)"));
				}
				int wordCount = countWords(textNode.asText());
				if (wordCount > maxWords) {
					problems.add(new ScenarioProblem(path + "/text",
							"Репліка '" + key + "' завдовга: " + wordCount + " слів (максимум " + maxWords + ")"));
				}
			}
			for (Map.Entry<String, JsonNode> field : node.properties()) {
				checkVoiceLines(field.getValue(), path + "/" + field.getKey(), firstPathByKey, maxWords, problems);
			}
		} else if (node.isArray()) {
			for (int i = 0; i < node.size(); i++) {
				checkVoiceLines(node.get(i), path + "/" + i, firstPathByKey, maxWords, problems);
			}
		}
	}

	private static int countWords(String text) {
		String trimmed = text.strip();
		return trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
	}

	/**
	 * Checks the two scene-level business rules: exactly one correct option/answer per choice or
	 * dialog scene, and the last non-demo (interactive) scene is the control scene.
	 */
	private void checkScenes(JsonNode scenes, List<ScenarioProblem> problems) {
		if (!scenes.isArray()) {
			return;
		}
		int lastInteractiveIndex = -1;
		for (int i = 0; i < scenes.size(); i++) {
			JsonNode scene = scenes.get(i);
			String type = scene.path("type").asText();
			String scenePath = "/scenes/" + i;

			if (!"demo".equals(type)) {
				lastInteractiveIndex = i;
			}

			String optionsField = switch (type) {
				case "choice" -> "options";
				case "dialog" -> "answers";
				default -> null;
			};
			if (optionsField != null) {
				checkExactlyOneCorrect(scene.path(optionsField), scenePath + "/" + optionsField, scene.path("key").asText(),
						problems);
			}
		}
		if (lastInteractiveIndex >= 0) {
			JsonNode lastInteractive = scenes.get(lastInteractiveIndex);
			if (!lastInteractive.path("is_control").asBoolean(false)) {
				problems.add(new ScenarioProblem("/scenes/" + lastInteractiveIndex + "/is_control",
						"Остання інтерактивна сцена має бути контрольною (is_control=true)"));
			}
		}
	}

	private void checkExactlyOneCorrect(JsonNode items, String itemsPath, String sceneKey,
			List<ScenarioProblem> problems) {
		if (!items.isArray()) {
			return;
		}
		int correctCount = 0;
		for (JsonNode item : items) {
			if (item.path("correct").asBoolean(false)) {
				correctCount++;
			}
		}
		if (correctCount != 1) {
			problems.add(new ScenarioProblem(itemsPath,
					"Сцена '" + sceneKey + "' має " + correctCount + " варіант(и/ів) із correct=true, очікується рівно 1"));
		}
	}

}
