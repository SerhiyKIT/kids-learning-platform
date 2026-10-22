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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Validates a lesson scenario against docs/lesson-scenario.schema.json (structural shape) plus
 * the business rules the schema can't express (see the schema file's own field descriptions and
 * docs/Формат_подання_уроків_та_ШІ.md §4.2). Standalone so both the admin content-entry endpoint
 * and the future LLM generation pipeline can reuse it.
 *
 * {@code @Component}, not {@code @Service}: this is a stateless, dependency-free validator, not
 * an orchestration service (no repositories, no other services) — see
 * docs/CONVENTIONS.md's naming rule (enforced by ArchUnitConventionsTest).
 */
@Component
public class ScenarioValidator {

	private static final String SCHEMA_CLASSPATH_LOCATION = "schema/lesson-scenario.schema.json";
	private static final int TIER_A_MAX_WORDS = 10;
	private static final int TIER_B_MAX_WORDS = 15;
	private static final String AGE_BAND_A = "age_3_4";
	/** A standalone three-digit number — the shape of every Ukrainian emergency number. */
	private static final Pattern THREE_DIGITS = Pattern.compile("(?<!\\d)\\d{3}(?!\\d)");

	private final Schema schema;
	private final ScenarioVocabulary vocabulary;
	private final StopWords stopWords = StopWords.load();

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
			this.vocabulary = ScenarioVocabulary.fromSchema(schemaNode);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** topic / age_band / pattern_ids vocabulary from the same schema this validator enforces. */
	public ScenarioVocabulary vocabulary() {
		return vocabulary;
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
			checkAgeBand(scenario, problems);
			for (String field : new String[] {"title", "learning_goal"}) {
				checkStopWords(scenario.path(field).asText(), field, "/" + field, problems);
			}
		}
		return problems;
	}

	/** Level A (age_3_4) allows ≤10 words per voice line; level B (age_5_6) allows ≤15. */
	private static int defaultMaxWordsPerVoiceLine(JsonNode scenario) {
		return AGE_BAND_A.equals(scenario.path("age_band").asText()) ? TIER_A_MAX_WORDS : TIER_B_MAX_WORDS;
	}

	/**
	 * docs/Промпт_генерації_уроків.md §3 rule 1, numbers from docs/Вимоги_модуль_Безпека.md §4:
	 * level A (age_3_4) — 3–4 scenes and 2 options to choose from; level B (age_5_6) — 5–6 scenes
	 * and 3 options. "Options" are a choice scene's options and a dialog's answers (QA1-033).
	 */
	private static void checkAgeBand(JsonNode scenario, List<ScenarioProblem> problems) {
		boolean levelA = AGE_BAND_A.equals(scenario.path("age_band").asText());
		int minScenes = levelA ? 3 : 5;
		int maxScenes = levelA ? 4 : 6;
		int options = levelA ? 2 : 3;
		String level = levelA ? "рівня А (3–4 роки)" : "рівня Б (5–6 років)";
		JsonNode scenes = scenario.path("scenes");
		if (scenes.size() < minScenes || scenes.size() > maxScenes) {
			problems.add(new ScenarioProblem("/scenes", "Для " + level + " потрібно " + minScenes + "–" + maxScenes
					+ " сцен, а їх " + scenes.size()));
		}
		for (int i = 0; i < scenes.size(); i++) {
			JsonNode scene = scenes.get(i);
			String field = switch (scene.path("type").asText()) {
				case "choice" -> "options";
				case "dialog" -> "answers";
				default -> null;
			};
			if (field != null && scene.path(field).size() != options) {
				problems.add(new ScenarioProblem("/scenes/" + i + "/" + field, "Для " + level + " потрібно " + options
						+ " варіанти, а їх " + scene.path(field).size()));
			}
		}
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
				checkNumbers(textNode.asText(), key, path, problems);
				checkStopWords(textNode.asText(), "Репліка '" + key + "'", path + "/text", problems);
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

	/**
	 * docs/Промпт_генерації_уроків.md §3 rule 9: a lesson names emergency numbers only from the
	 * verified table (EmergencyNumber). Applied to every topic — a road-safety lesson must not
	 * carry an invented number either. Which service a verified number is paired with is left to
	 * the human moderator.
	 */
	private static void checkNumbers(String text, String key, String path, List<ScenarioProblem> problems) {
		Matcher matcher = THREE_DIGITS.matcher(text);
		while (matcher.find()) {
			if (!EmergencyNumber.isVerified(matcher.group())) {
				problems.add(new ScenarioProblem(path + "/text", "Репліка '" + key + "' називає номер " + matcher.group()
						+ ", якого немає в перевіреній таблиці (101, 102, 103, 104, 112)"));
			}
		}
	}

	/** docs/Промпт_генерації_уроків.md §3 rule 8: nothing from the stop list reaches the moderator's queue. */
	private void checkStopWords(String text, String what, String path, List<ScenarioProblem> problems) {
		for (String hit : stopWords.find(text)) {
			problems.add(new ScenarioProblem(path, what + " містить заборонене слово «" + hit + "» (стоп-словник)"));
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
		for (int i = 0; i < items.size(); i++) {
			JsonNode item = items.get(i);
			if (item.path("correct").asBoolean(false)) {
				correctCount++;
				// Prompt §3 rule 6: a right answer never comes with a "consequence" (QA1-033).
				String consequence = item.path("feedback").path("consequence_animation").asText("none");
				if (!"none".equals(consequence)) {
					problems.add(new ScenarioProblem(itemsPath + "/" + i + "/feedback/consequence_animation",
							"Правильна відповідь не може мати наслідку (" + consequence + "), лише none"));
				}
			}
		}
		if (correctCount != 1) {
			problems.add(new ScenarioProblem(itemsPath,
					"Сцена '" + sceneKey + "' має " + correctCount + " варіант(и/ів) із correct=true, очікується рівно 1"));
		}
	}

}
