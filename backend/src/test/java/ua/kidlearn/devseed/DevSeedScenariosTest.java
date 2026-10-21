package ua.kidlearn.devseed;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import ua.kidlearn.scenario.ScenarioValidator;

class DevSeedScenariosTest {

	private final ObjectMapper objectMapper = JsonMapper.builder().build();

	@Test
	void allSceneTypesFixtureIsValidAndCoversEveryType() {
		JsonNode scenario = objectMapper.readTree(DevSeedScenarios.ALL_SCENE_TYPES);

		assertThat(new ScenarioValidator(objectMapper).validate(scenario)).isEmpty();
		List<String> types = new ArrayList<>();
		scenario.get("scenes").forEach(scene -> types.add(scene.get("type").asText()));
		assertThat(types).contains("demo", "choice", "sorting", "dialog");
	}

}
