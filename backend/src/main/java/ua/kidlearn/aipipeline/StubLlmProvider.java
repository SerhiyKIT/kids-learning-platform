package ua.kidlearn.aipipeline;

import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Deterministic fake LLM: no network, no API key, always returns the same fixed, schema-valid
 * scenario. The only implementation for now — final provider choice (Claude / GPT / Gemini) is
 * pending the Ukrainian-quality test (docs/Промпт_генерації_уроків.md §5).
 *
 * TODO: add a real provider implementation that builds the prompt from
 * docs/Промпт_генерації_уроків.md (system prompt + PATTERN/AGE_BAND/TOPIC/VERIFIED_FACTS/
 * ASSET_REGISTRY/JSON_SCHEMA user message) and the given GenerationRequest, calling out to
 * Claude/GPT/Gemini with the API key read from an environment variable (never commit it), and
 * make it the active LlmProvider in dev/prod — mirroring how MinioAudioStorage/InMemoryAudioStorage
 * are split between real and test wiring.
 */
@Component
class StubLlmProvider implements LlmProvider {

	private static final String MODEL_ID = "stub";

	private static final String FIXED_VALID_SCENARIO = """
			{
			  "schema_version": "1.0.0",
			  "module": "safety",
			  "topic": "road_safety",
			  "pattern_ids": ["1.6"],
			  "age_band": "age_5_6",
			  "title": "Crossing the street",
			  "learning_goal": "Look both ways before crossing",
			  "reality_bridge": {"key": "bridge1", "text": "Remember to look both ways"},
			  "scenes": [
			    {
			      "key": "scene_1", "type": "demo", "background": "street_bg",
			      "characters": [{"id": "guide", "emotion": "neutral"}],
			      "narration": [
			        {"line": {"key": "narr1", "text": "Watch me cross the street safely"}, "action": "walk_to_road"}
			      ]
			    },
			    {
			      "key": "scene_2", "type": "choice", "background": "street_bg",
			      "characters": [{"id": "guide", "emotion": "thinking"}],
			      "is_control": false,
			      "setup": {"key": "setup1", "text": "What should you do before crossing"},
			      "options": [
			        {"id": "a", "icon": "icon_look", "label": {"key": "opt_a", "text": "Look both ways"}, "correct": true,
			         "feedback": {"line": {"key": "fb_a", "text": "Great job looking both ways"}}},
			        {"id": "b", "icon": "icon_run", "label": {"key": "opt_b", "text": "Run across fast"}, "correct": false,
			         "feedback": {"line": {"key": "fb_b", "text": "Wait, always look first"}}}
			      ],
			      "assistant": {
			        "hints": [
			          {"level": 1, "line": {"key": "hint1", "text": "Think about safety first"}},
			          {"level": 2, "line": {"key": "hint2", "text": "Look left and right"}}
			        ],
			        "re_explain": {"key": "re1", "text": "Always look both ways first"}
			      }
			    },
			    {
			      "key": "scene_3", "type": "choice", "background": "street_bg",
			      "characters": [{"id": "guide", "emotion": "pride"}],
			      "is_control": true,
			      "setup": {"key": "setup2", "text": "Now show me what to do"},
			      "options": [
			        {"id": "a", "icon": "icon_look", "label": {"key": "opt_c", "text": "Look both ways"}, "correct": true,
			         "feedback": {"line": {"key": "fb_c", "text": "Perfect, you are safe now"}}},
			        {"id": "b", "icon": "icon_run", "label": {"key": "opt_d", "text": "Cross without looking"}, "correct": false,
			         "feedback": {"line": {"key": "fb_d", "text": "Careful, look before crossing"}}}
			      ],
			      "assistant": {
			        "hints": [
			          {"level": 1, "line": {"key": "hint3", "text": "Remember what we practiced"}},
			          {"level": 2, "line": {"key": "hint4", "text": "Look both ways first"}}
			        ],
			        "re_explain": {"key": "re2", "text": "Look both ways before crossing"}
			      }
			    }
			  ]
			}
			""";

	private final ObjectMapper objectMapper;
	private final AtomicReference<JsonNode> forcedScenario = new AtomicReference<>();

	StubLlmProvider(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public GeneratedScenario generate(GenerationRequest request) {
		JsonNode scenario = forcedScenario.get();
		if (scenario == null) {
			scenario = objectMapper.readTree(FIXED_VALID_SCENARIO);
		}
		return new GeneratedScenario(scenario, MODEL_ID);
	}

	/** Test hook: makes every call return {@code scenario} instead of the fixed default, until cleared. */
	void forceNextScenario(JsonNode scenario) {
		forcedScenario.set(scenario);
	}

	/** Test hook: reverts to the fixed default scenario. */
	void clearForcedScenario() {
		forcedScenario.set(null);
	}

}
