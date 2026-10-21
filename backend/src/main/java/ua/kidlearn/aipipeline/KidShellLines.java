package ua.kidlearn.aipipeline;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * The guide's fixed lines around lessons in child mode (shelf greeting, "turn the device", lesson
 * end, rest). Written by people and reviewed with the code — never AI-generated — so an admin voices
 * them directly (AdminVoicingController) without the lesson moderation flow. Keys are what the
 * frontend asks for; texts follow the lesson voice-line rules (simple Ukrainian, ≤ 10 words).
 */
public final class KidShellLines {

	public static final Map<String, String> LINES;

	static {
		Map<String, String> lines = new LinkedHashMap<>();
		lines.put("greet_1", "Привіт! Я Тоша. Обирай урок!");
		lines.put("greet_2", "Ура, ти тут! Пограймося разом?");
		lines.put("greet_3", "Привіт, друже! Який урок сьогодні?");
		lines.put("turn_device", "Поверни, будь ласка, пристрій — ось так!");
		lines.put("celebrate", "Ура! Урок пройдено! Молодець!");
		lines.put("sticker_new", "Тримай наліпку! Вона тепер у твоєму альбомі.");
		lines.put("replay_joy", "Ура, ми повторили! Я дуже радий!");
		lines.put("album_intro", "Ось твої наліпки! Торкнись — і я нагадаю.");
		lines.put("sleep_tired", "Ми сьогодні молодці! Пограймося згодом.");
		lines.put("sleep_again", "Ш-ш-ш… Тоша спить. Пограймося згодом.");
		// Style picker (ua.kidlearn.themes): the three questions, each choice said out loud, the wait.
		lines.put("style_color", "Який колір тобі найбільше подобається?");
		lines.put("style_world", "А в якому світі тобі цікаво?");
		lines.put("style_mood", "Хочеш спокійно чи яскраво?");
		lines.put("style_drawing", "Я вже малюю тобі кімнату!");
		lines.put("theme_ready", "Дивись! Твоя нова кімната вже готова!");
		lines.put("color_red", "Червоний!");
		lines.put("color_orange", "Помаранчевий!");
		lines.put("color_yellow", "Жовтий!");
		lines.put("color_green", "Зелений!");
		lines.put("color_blue", "Синій!");
		lines.put("color_purple", "Фіолетовий!");
		lines.put("world_space", "Космос!");
		lines.put("world_sea", "Море!");
		lines.put("world_forest", "Ліс!");
		lines.put("world_dinosaurs", "Динозаври!");
		lines.put("world_flowers", "Квіти!");
		lines.put("world_cars", "Машинки!");
		lines.put("mood_calm", "Спокійно.");
		lines.put("mood_bright", "Яскраво!");
		LINES = Collections.unmodifiableMap(lines);
	}

	private KidShellLines() {
	}

	/** Distinct texts in catalog order, for voicing. */
	public static Set<String> texts() {
		return new LinkedHashSet<>(LINES.values());
	}

}
