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
