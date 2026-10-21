package ua.kidlearn.themes;

import java.util.List;
import java.util.Optional;

/**
 * The closed lists a child chooses from, and the palette tokens a theme consists of — the single
 * source of truth (mirrored by frontend/src/lib/theme-catalog.ts, documented in
 * docs/Специфікація_ассетів.md "Теми персоналізації"). A child's answers are only ever ids from
 * these lists; nothing free-form reaches storage or the LLM.
 */
public final class ThemeCatalog {

	public static final List<String> COLOR_IDS = List.of("red", "orange", "yellow", "green", "blue", "purple");

	/** Each world is a decor pack (placeholder patterns until the M4 illustrations). */
	public static final List<String> WORLD_IDS = List.of("space", "sea", "forest", "dinosaurs", "flowers", "cars");

	public static final List<String> MOOD_IDS = List.of("calm", "bright");

	public static final List<String> GUIDE_ACCESSORIES = List.of("none", "cap", "scarf", "helmet", "crown_flowers",
			"goggles");

	/**
	 * Every theme sets exactly these child-mode tokens (frontend: --color-kid-&lt;token&gt;) and nothing
	 * else. Shadows are derived from them on the frontend.
	 */
	public static final List<String> PALETTE_TOKENS = List.of("canvas", "bg-top", "bg-bottom", "sun", "sun-deep",
			"teal", "ink", "ink-soft", "pip", "glow", "correct", "correct-surface", "scroll", "guide-main",
			"guide-accent");

	/** The stage background tokens (contrast is checked against all three). */
	public static final List<String> BACKGROUND_TOKENS = List.of("canvas", "bg-top", "bg-bottom");

	private ThemeCatalog() {
	}

	public static boolean isValid(String colorId, String worldId, String moodId) {
		return COLOR_IDS.contains(colorId) && WORLD_IDS.contains(worldId) && MOOD_IDS.contains(moodId);
	}

	/** "blue|space|calm" — deterministic, so at most 6 × 6 × 2 = 72 themes ever exist. */
	public static String preferenceKey(String colorId, String worldId, String moodId) {
		return colorId + "|" + worldId + "|" + moodId;
	}

	public static Optional<StyleAnswers> parseKey(String preferenceKey) {
		String[] parts = preferenceKey == null ? new String[0] : preferenceKey.split("\\|", -1);
		if (parts.length != 3 || !isValid(parts[0], parts[1], parts[2])) {
			return Optional.empty();
		}
		return Optional.of(new StyleAnswers(parts[0], parts[1], parts[2]));
	}

}
