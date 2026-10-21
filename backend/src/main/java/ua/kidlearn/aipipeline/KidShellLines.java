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
		LINES = Collections.unmodifiableMap(lines);
	}

	private KidShellLines() {
	}

	/** Distinct texts in catalog order, for voicing. */
	public static Set<String> texts() {
		return new LinkedHashSet<>(LINES.values());
	}

}
