package ua.kidlearn.aipipeline;

import java.util.concurrent.atomic.AtomicReference;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Deterministic fake LLM: no network, no API key, always returns the same fixed, schema-valid
 * scenario — with every interactive scene type the child engine plays except dialog (demo, choice,
 * sorting, and a control choice), so the end-to-end tests can play them all on generated content. Active whenever the real provider isn't (see {@link AnthropicLlmCondition}): the
 * default in dev and the only provider the test suite and CI ever see — mirroring how
 * MinioAudioStorage/InMemoryAudioStorage are split between real and test wiring.
 */
@Component
@Conditional(AnthropicLlmCondition.Inverse.class)
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
			      "key": "scene_3", "type": "sorting", "is_control": false, "background": "street_bg",
			      "characters": [{"id": "guide", "emotion": "thinking"}],
			      "setup": {"key": "sort_setup", "text": "Put each picture where it belongs"},
			      "categories": [
			        {"id": "cat_1", "icon": "icon_safe", "label": {"key": "sort_cat1", "text": "Safe"}},
			        {"id": "cat_2", "icon": "icon_danger", "label": {"key": "sort_cat2", "text": "Not safe"}}
			      ],
			      "items": [
			        {"icon": "item_hold_hand", "target": "cat_1",
			         "feedback_correct": {"key": "sort_i1_ok", "text": "Yes, holding a hand is safe"},
			         "feedback_wrong": {"key": "sort_i1_no", "text": "Holding a hand is safe"}},
			        {"icon": "item_ball_road", "target": "cat_2",
			         "feedback_correct": {"key": "sort_i2_ok", "text": "Yes, running after a ball is not safe"},
			         "feedback_wrong": {"key": "sort_i2_no", "text": "Running after a ball is not safe"}},
			        {"icon": "item_zebra", "target": "cat_1",
			         "feedback_correct": {"key": "sort_i3_ok", "text": "Yes, the zebra crossing is safe"},
			         "feedback_wrong": {"key": "sort_i3_no", "text": "The zebra crossing is safe"}}
			      ],
			      "assistant": {
			        "hints": [
			          {"level": 1, "line": {"key": "sort_h1", "text": "Would a grown-up do this"}},
			          {"level": 2, "line": {"key": "sort_h2", "text": "Near cars without a grown-up is not safe"}}
			        ],
			        "re_explain": {"key": "sort_re", "text": "Put every picture in the right basket"}
			      }
			    },
			    {
			      "key": "scene_4", "type": "choice", "background": "street_bg",
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
