package ua.kidlearn.scenario;

/** Shared schema-valid scenario JSON fixtures, reused by tests across packages that just need
 * "some valid lesson version content" and don't care about its specific shape. */
public final class ScenarioFixtures {

	/** 3 scenes (demo + choice + control choice): the smallest shape the schema + business rules allow. */
	public static final String VALID_MINIMAL_SCENARIO = """
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

	private ScenarioFixtures() {
	}

}
