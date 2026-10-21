package ua.kidlearn.devseed;

/**
 * Dev-only lesson fixtures for manual testing of the child-mode scene engine. Must stay valid for
 * docs/lesson-scenario.schema.json and ScenarioValidator (checked by DevSeedScenariosTest, and again
 * by DevSeedService before seeding).
 */
final class DevSeedScenarios {

	/**
	 * One scene of every type the schema defines — demo, choice, sorting, dialog — ending in a
	 * control choice (no hints there), age 5–6 (≤ 15 words per line).
	 */
	static final String ALL_SCENE_TYPES = """
			{
			  "schema_version": "1.0.0",
			  "module": "safety",
			  "topic": "road_safety",
			  "pattern_ids": ["1.6", "1.7"],
			  "age_band": "age_5_6",
			  "title": "Демо: усі типи сцен",
			  "learning_goal": "Дитина відрізняє безпечні й небезпечні дії біля дороги",
			  "reality_bridge": {"key": "bridge", "text": "Коли гуляєш біля дороги з мамою чи татом — роби так само!"},
			  "scenes": [
			    {
			      "key": "scene_1", "type": "demo", "background": "yard_day",
			      "characters": [{"id": "fox_guide", "emotion": "joy", "pose": "stand"}],
			      "props": ["ball_red"],
			      "narration": [
			        {"line": {"key": "n1", "text": "Привіт! Я Тоша. Сьогодні вчимося гратися безпечно біля дороги."}, "action": "wave"},
			        {"line": {"key": "n2", "text": "Ой! Мій м'ячик покотився до дороги."}, "action": "stop"}
			      ]
			    },
			    {
			      "key": "scene_2", "type": "choice", "is_control": false, "background": "street_edge",
			      "characters": [{"id": "fox_guide", "emotion": "surprise", "pose": "stand"}],
			      "props": ["ball_red", "car_blue"],
			      "setup": {"key": "s2_setup", "text": "М'ячик на дорозі. Що робити Тоші?"},
			      "options": [
			        {"id": "a", "icon": "icon_run_to_road", "label": {"key": "s2_a", "text": "Побігти за м'ячиком"}, "correct": false,
			         "feedback": {"line": {"key": "s2_fa", "text": "Ой! Там їдуть машини. Бігти не можна. Спробуй ще!"}, "consequence_animation": "car_horn_stop"}},
			        {"id": "b", "icon": "icon_call_adult", "label": {"key": "s2_b", "text": "Покликати дорослого"}, "correct": true,
			         "feedback": {"line": {"key": "s2_fb", "text": "Так! Дорослий безпечно дістане м'ячик."}}},
			        {"id": "c", "icon": "icon_wait_alone", "label": {"key": "s2_c", "text": "Чекати біля дороги самому"}, "correct": false,
			         "feedback": {"line": {"key": "s2_fc", "text": "Ой, самому біля дороги не можна. Спробуй ще!"}, "consequence_animation": "guide_oops"}}
			      ],
			      "assistant": {
			        "hints": [
			          {"level": 1, "line": {"key": "s2_h1", "text": "Хто може безпечно дістати м'ячик?"}},
			          {"level": 2, "line": {"key": "s2_h2", "text": "Поклич дорослого — він допоможе."}}
			        ],
			        "re_explain": {"key": "s2_re", "text": "На дорозі машини. Туди бігти не можна, клич дорослого."}
			      }
			    },
			    {
			      "key": "scene_3", "type": "sorting", "is_control": false, "background": "street_edge",
			      "characters": [{"id": "fox_guide", "emotion": "thinking", "pose": "point"}],
			      "setup": {"key": "s3_setup", "text": "Допоможи Тоші: що безпечно, а що небезпечно біля дороги?"},
			      "categories": [
			        {"id": "cat_1", "icon": "icon_safe", "label": {"key": "s3_cat1", "text": "Безпечно"}},
			        {"id": "cat_2", "icon": "icon_danger", "label": {"key": "s3_cat2", "text": "Небезпечно"}}
			      ],
			      "items": [
			        {"icon": "item_hold_hand", "target": "cat_1",
			         "feedback_correct": {"key": "s3_i1_ok", "text": "Так! Тримати дорослого за руку — безпечно."},
			         "feedback_wrong": {"key": "s3_i1_no", "text": "Ой! За руку з дорослим — це безпечно."}},
			        {"icon": "item_ball_road", "target": "cat_2",
			         "feedback_correct": {"key": "s3_i2_ok", "text": "Так! Бігти за м'ячем на дорогу небезпечно."},
			         "feedback_wrong": {"key": "s3_i2_no", "text": "Ой! За м'ячем на дорогу бігти небезпечно."}},
			        {"icon": "item_zebra", "target": "cat_1",
			         "feedback_correct": {"key": "s3_i3_ok", "text": "Так! Зебра з дорослим — безпечно."},
			         "feedback_wrong": {"key": "s3_i3_no", "text": "Ой! Перехід по зебрі з дорослим — безпечно."}},
			        {"icon": "item_play_road", "target": "cat_2",
			         "feedback_correct": {"key": "s3_i4_ok", "text": "Так! Гратися біля дороги небезпечно."},
			         "feedback_wrong": {"key": "s3_i4_no", "text": "Ой! Біля дороги гратися небезпечно."}}
			      ],
			      "assistant": {
			        "hints": [
			          {"level": 1, "line": {"key": "s3_h1", "text": "Подумай: чи міг би так зробити дорослий?"}},
			          {"level": 2, "line": {"key": "s3_h2", "text": "Усе, що біля машин без дорослого, — небезпечно."}}
			        ],
			        "re_explain": {"key": "s3_re", "text": "Поклади кожну картинку в кошик: зелений — безпечно, жовтий — небезпечно."}
			      }
			    },
			    {
			      "key": "scene_4", "type": "dialog", "is_control": false, "background": "yard_day",
			      "characters": [{"id": "fox_guide", "emotion": "thinking", "pose": "stand"}],
			      "question": {"key": "s4_q", "text": "Кого треба покликати, якщо м'ячик на дорозі?"},
			      "answers": [
			        {"id": "a", "icon": "icon_adult", "label": {"key": "s4_a", "text": "Дорослого"}, "correct": true,
			         "feedback": {"key": "s4_fa", "text": "Так! Дорослий допоможе безпечно."}},
			        {"id": "b", "icon": "icon_cat", "label": {"key": "s4_b", "text": "Котика"}, "correct": false,
			         "feedback": {"key": "s4_fb", "text": "Котик не допоможе. Спробуй ще!"}},
			        {"id": "c", "icon": "icon_friend", "label": {"key": "s4_c", "text": "Маленького друга"}, "correct": false,
			         "feedback": {"key": "s4_fc", "text": "Другові теж не можна на дорогу. Спробуй ще!"}}
			      ],
			      "assistant": {
			        "hints": [
			          {"level": 1, "line": {"key": "s4_h1", "text": "Хто вміє безпечно переходити дорогу?"}},
			          {"level": 2, "line": {"key": "s4_h2", "text": "Це мама, тато або вихователька."}}
			        ],
			        "re_explain": {"key": "s4_re", "text": "Коли щось на дорозі — клич дорослого."}
			      }
			    },
			    {
			      "key": "scene_5", "type": "choice", "is_control": true, "background": "yard_day",
			      "characters": [{"id": "fox_guide", "emotion": "pride", "pose": "stand"}],
			      "props": ["ball_red"],
			      "setup": {"key": "s5_setup", "text": "М'ячик знову на дорозі. Що зробиш ти?"},
			      "options": [
			        {"id": "a", "icon": "icon_call_adult", "label": {"key": "s5_a", "text": "Покличу дорослого"}, "correct": true,
			         "feedback": {"line": {"key": "s5_fa", "text": "Молодець! Ти знаєш, як безпечно!"}}},
			        {"id": "b", "icon": "icon_run_to_road", "label": {"key": "s5_b", "text": "Побіжу сам"}, "correct": false,
			         "feedback": {"line": {"key": "s5_fb", "text": "Ой, ні! Клич дорослого. Спробуй ще!"}, "consequence_animation": "guide_oops"}},
			        {"id": "c", "icon": "icon_kick", "label": {"key": "s5_c", "text": "Штовхну ногою"}, "correct": false,
			         "feedback": {"line": {"key": "s5_fc", "text": "Ой! До дороги підходити не можна. Спробуй ще!"}, "consequence_animation": "guide_startled"}}
			      ],
			      "assistant": {
			        "hints": [
			          {"level": 1, "line": {"key": "s5_h1", "text": "Згадай, як ми робили щойно."}},
			          {"level": 2, "line": {"key": "s5_h2", "text": "Дорослий дістане м'ячик безпечно."}}
			        ],
			        "re_explain": {"key": "s5_re", "text": "За м'ячиком на дорогу бігти не можна."}
			      }
			    }
			  ]
			}
			""";

	private DevSeedScenarios() {
	}

}
