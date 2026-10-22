package ua.kidlearn.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** No Spring context needed: ScenarioValidator only depends on an ObjectMapper. */
class ScenarioValidatorTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final ScenarioValidator validator = new ScenarioValidator(objectMapper);

	private JsonNode parse(String json) {
		return objectMapper.readTree(json);
	}

	@Test
	void validMinimalScenarioHasNoProblems() {
		List<ScenarioProblem> problems = validator.validate(parse(ScenarioFixtures.VALID_MINIMAL_SCENARIO));
		assertThat(problems).isEmpty();
	}

	@Test
	void missingRequiredFieldIsRejected() {
		JsonNode scenario = parse(ScenarioFixtures.VALID_MINIMAL_SCENARIO);
		JsonNode withoutTitle = ((tools.jackson.databind.node.ObjectNode) scenario.deepCopy()).without("title");

		List<ScenarioProblem> problems = validator.validate(withoutTitle);

		assertThat(problems).isNotEmpty();
	}

	@Test
	void wrongEnumValueIsRejected() {
		JsonNode scenario = parse(ScenarioFixtures.VALID_MINIMAL_SCENARIO);
		JsonNode wrongTopic = ((tools.jackson.databind.node.ObjectNode) scenario.deepCopy())
				.put("topic", "not_a_real_topic");

		List<ScenarioProblem> problems = validator.validate(wrongTopic);

		assertThat(problems).isNotEmpty();
	}

	@Test
	void extraPropertyIsRejected() {
		JsonNode scenario = parse(ScenarioFixtures.VALID_MINIMAL_SCENARIO);
		JsonNode withExtra = ((tools.jackson.databind.node.ObjectNode) scenario.deepCopy())
				.put("unexpected_field", "nope");

		List<ScenarioProblem> problems = validator.validate(withExtra);

		assertThat(problems).isNotEmpty();
	}

	@Test
	void anEmergencyNumberOutsideTheVerifiedTableIsRejected() {
		String scenario = ScenarioFixtures.VALID_MINIMAL_SCENARIO.replace("\"Look both ways\"", "\"Дзвони 109\"");

		List<ScenarioProblem> problems = validator.validate(parse(scenario));

		assertThat(problems).anyMatch(p -> p.path().endsWith("/label/text") && p.message().contains("109"));
	}

	@Test
	void verifiedEmergencyNumbersPass() {
		String scenario = ScenarioFixtures.VALID_MINIMAL_SCENARIO
				.replace("\"Look both ways\"", "\"Дзвони 101 або 112\"")
				.replace("\"Run across fast\"", "\"Дзвони 103\"");

		assertThat(validator.validate(parse(scenario))).isEmpty();
	}

	@Test
	void aStopListWordInAVoiceLineOrTheLearningGoalIsRejected() {
		String scenario = ScenarioFixtures.VALID_MINIMAL_SCENARIO
				.replace("\"Run across fast\"", "\"Побігти й побачити кров\"")
				.replace("\"learning_goal\": \"Look both ways before crossing\"",
						"\"learning_goal\": \"Інакше страшно буде\"");

		List<ScenarioProblem> problems = validator.validate(parse(scenario));

		assertThat(problems).anyMatch(p -> p.path().endsWith("/label/text") && p.message().contains("«кров»"));
		assertThat(problems).anyMatch(p -> p.path().equals("/learning_goal") && p.message().contains("«страшно буде»"));
	}

	@Test
	void inflectedStopWordsAreCaughtButTheVerifiedServiceNamesAreNot() {
		String scenario = ScenarioFixtures.VALID_MINIMAL_SCENARIO
				.replace("\"Run across fast\"", "\"Без крові й без смерті\"")
				.replace("\"Look both ways\"", "\"Аварійна служба газу: 104\"");

		List<ScenarioProblem> problems = validator.validate(parse(scenario));

		assertThat(problems).extracting(ScenarioProblem::message)
				.anyMatch(m -> m.contains("«крові»"))
				.anyMatch(m -> m.contains("«смерті»"))
				.noneMatch(m -> m.contains("аварійна"));
	}

	@Test
	void levelAWantsTwoOptionsAndLevelBWantsFiveToSixScenesWithThree() {
		// Level A with a third option in its choice.
		String threeOptions = ScenarioFixtures.VALID_MINIMAL_SCENARIO.replace(
				"{\"id\": \"b\", \"icon\": \"icon_run\"",
				"{\"id\": \"c\", \"icon\": \"icon_wait\", \"label\": {\"key\": \"opt_c3\", \"text\": \"Wait\"}, \"correct\": false,"
						+ " \"feedback\": {\"line\": {\"key\": \"fb_c3\", \"text\": \"Wait for a grown-up\"}}},"
						+ " {\"id\": \"b\", \"icon\": \"icon_run\"");
		assertThat(validator.validate(parse(threeOptions)))
				.anyMatch(p -> p.path().endsWith("/options") && p.message().contains("рівня А") && p.message().contains("2 варіанти"));

		// The same 3-scene, 2-option lesson labelled level B.
		String levelB = ScenarioFixtures.VALID_MINIMAL_SCENARIO.replace("\"age_band\": \"age_3_4\"", "\"age_band\": \"age_5_6\"");
		assertThat(validator.validate(parse(levelB)))
				.anyMatch(p -> p.path().equals("/scenes") && p.message().contains("5–6 сцен"))
				.anyMatch(p -> p.path().endsWith("/options") && p.message().contains("3 варіанти"));
	}

	@Test
	void aRightAnswerWithAConsequenceIsRejected() {
		String scenario = ScenarioFixtures.VALID_MINIMAL_SCENARIO.replace(
				"\"feedback\": {\"line\": {\"key\": \"fb_a\", \"text\": \"Great job looking both ways\"}}",
				"\"feedback\": {\"line\": {\"key\": \"fb_a\", \"text\": \"Great job looking both ways\"}, \"consequence_animation\": \"car_horn_stop\"}");
		assertThat(scenario).isNotEqualTo(ScenarioFixtures.VALID_MINIMAL_SCENARIO);

		assertThat(validator.validate(parse(scenario)))
				.anyMatch(p -> p.path().endsWith("/feedback/consequence_animation") && p.message().contains("car_horn_stop"));
	}

	@Test
	void duplicateVoiceLineKeyIsRejected() {
		String scenario = ScenarioFixtures.VALID_MINIMAL_SCENARIO.replace("\"hint2\"", "\"hint1\"");

		List<ScenarioProblem> problems = validator.validate(parse(scenario));

		assertThat(problems).isNotEmpty();
		assertThat(problems).anyMatch(p -> p.message().contains("hint1"));
	}

	@Test
	void twoCorrectOptionsIsRejected() {
		String scenario = ScenarioFixtures.VALID_MINIMAL_SCENARIO.replace(
				"\"id\": \"b\", \"icon\": \"icon_run\", \"label\": {\"key\": \"opt_b\", \"text\": \"Run across fast\"}, \"correct\": false",
				"\"id\": \"b\", \"icon\": \"icon_run\", \"label\": {\"key\": \"opt_b\", \"text\": \"Run across fast\"}, \"correct\": true");

		List<ScenarioProblem> problems = validator.validate(parse(scenario));

		assertThat(problems).isNotEmpty();
	}

	@Test
	void lastInteractiveSceneNotControlIsRejected() {
		String scenario = ScenarioFixtures.VALID_MINIMAL_SCENARIO
				.replace("\"key\": \"scene_3\", \"type\": \"choice\", \"background\": \"street_bg\",\n"
						+ "      \"characters\": [{\"id\": \"guide\", \"emotion\": \"pride\"}],\n"
						+ "      \"is_control\": true,",
						"\"key\": \"scene_3\", \"type\": \"choice\", \"background\": \"street_bg\",\n"
						+ "      \"characters\": [{\"id\": \"guide\", \"emotion\": \"pride\"}],\n"
						+ "      \"is_control\": false,");

		List<ScenarioProblem> problems = validator.validate(parse(scenario));

		assertThat(problems).isNotEmpty();
		assertThat(problems).anyMatch(p -> p.path().contains("is_control"));
	}

	@Test
	void overLengthVoiceLineIsRejected() {
		String scenario = ScenarioFixtures.VALID_MINIMAL_SCENARIO.replace(
				"\"key\": \"narr1\", \"text\": \"Watch me cross the street safely\"",
				"\"key\": \"narr1\", \"text\": \"Watch me cross the busy street very safely every single time we go outside\"");

		List<ScenarioProblem> problems = validator.validate(parse(scenario));

		assertThat(problems).isNotEmpty();
		assertThat(problems).anyMatch(p -> p.message().contains("завдовга"));
	}

}
