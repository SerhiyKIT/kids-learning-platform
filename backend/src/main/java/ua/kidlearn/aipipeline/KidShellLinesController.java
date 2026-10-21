package ua.kidlearn.aipipeline;

import java.util.ArrayList;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;

/**
 * The guide's fixed child-mode lines with their audioUrl where voiced (same lookup as a lesson's
 * lines). Not child data — just content, like /api/audio.
 */
@RestController
@PreAuthorize("hasRole('PARENT')")
public class KidShellLinesController {

	private final ScenarioAudioEnricher scenarioAudioEnricher;

	public KidShellLinesController(ScenarioAudioEnricher scenarioAudioEnricher) {
		this.scenarioAudioEnricher = scenarioAudioEnricher;
	}

	@GetMapping("/api/kid/shell-lines")
	public List<JsonNode> lines() {
		ArrayNode lines = JsonNodeFactory.instance.arrayNode();
		KidShellLines.LINES.forEach((key, text) -> lines.addObject().put("key", key).put("text", text));
		List<JsonNode> enriched = new ArrayList<>();
		scenarioAudioEnricher.withAudioUrls(lines).forEach(enriched::add);
		return enriched;
	}

}
