package ua.kidlearn.themes;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import ua.kidlearn.aipipeline.AnthropicLlmCondition;

/**
 * Deterministic theme generator: no network, no key. The palette is computed in HSL from the
 * answers — hue from the colour, saturation from the mood, light calm backgrounds, and the
 * interactive colours darkened until they reach the validator's contrast — so its themes always
 * pass ThemeValidator. Active whenever AnthropicThemeGenerator isn't.
 */
@Component
@Conditional(AnthropicLlmCondition.Inverse.class)
class StubThemeGenerator implements ThemeGenerator {

	private static final Map<String, Integer> HUES = Map.of("red", 355, "orange", 25, "yellow", 48, "green", 135,
			"blue", 210, "purple", 275);
	private static final Map<String, String> ACCESSORIES = Map.of("space", "helmet", "sea", "goggles", "forest",
			"scarf", "dinosaurs", "cap", "flowers", "crown_flowers", "cars", "cap");
	private static final Map<String, String> COLOR_NAMES = Map.of("red", "Червона", "orange", "Помаранчева", "yellow",
			"Жовта", "green", "Зелена", "blue", "Синя", "purple", "Фіолетова");
	private static final Map<String, String> WORLD_NAMES = Map.of("space", "космос", "sea", "море", "forest", "ліс",
			"dinosaurs", "динозаври", "flowers", "квіти", "cars", "машинки");
	// Margin over the validator's 3:1, so rounding never puts a stub theme on the edge.
	private static final double TARGET_UI_CONTRAST = 3.2;

	@Override
	public GeneratedTheme generate(String colorId, String worldId, String moodId) {
		int hue = HUES.get(colorId);
		boolean bright = "bright".equals(moodId);
		double s = bright ? 0.8 : 0.45;

		Map<String, String> palette = new LinkedHashMap<>();
		palette.put("bg-top", hex(hue, s, 0.96));
		palette.put("canvas", hex(hue, s, 0.92));
		palette.put("bg-bottom", hex(hue, s * 0.9, 0.85));
		palette.put("ink", hex(hue, 0.45, 0.18));
		palette.put("ink-soft", hex(hue, 0.3, 0.36));
		palette.put("sun", darkEnough(hue, bright ? 0.75 : 0.5, palette));
		palette.put("sun-deep", hex(hue, 0.6, 0.97));
		palette.put("teal", darkEnough(secondaryHue(hue), bright ? 0.6 : 0.4, palette));
		palette.put("pip", hex(hue, 0.35, 0.8));
		palette.put("glow", hex(hue, s, 0.8));
		palette.put("correct", hex(140, 0.5, 0.7));
		palette.put("correct-surface", hex(140, 0.6, 0.94));
		palette.put("scroll", hex(hue, 0.5, 0.72));
		palette.put("guide-main", hex((hue + 30) % 360, 0.45, 0.8));
		palette.put("guide-accent", hex((hue + 30) % 360, 0.5, 0.68));

		String name = COLOR_NAMES.get(colorId) + " тема: " + WORLD_NAMES.get(worldId) + (bright ? ", яскраво" : ", спокійно");
		return new GeneratedTheme(name, palette, ACCESSORIES.get(worldId), "stub");
	}

	/**
	 * The "далі"/album colour: the complementary hue, moved off red — a red button reads as "danger"
	 * in child mode (docs/UX_гайд_дитячого_режиму.md: no alarming signals).
	 */
	private static int secondaryHue(int hue) {
		int complement = (hue + 180) % 360;
		return complement >= 340 || complement <= 20 ? 30 : complement;
	}

	/** The lightest shade of this hue that still has the UI contrast against every background. */
	private static String darkEnough(int hue, double saturation, Map<String, String> palette) {
		for (double l = 0.55; l > 0.1; l -= 0.01) {
			String candidate = hex(hue, saturation, l);
			boolean enough = ThemeCatalog.BACKGROUND_TOKENS.stream()
					.allMatch(bg -> ThemeValidator.contrastRatio(candidate, palette.get(bg)) >= TARGET_UI_CONTRAST);
			if (enough) {
				return candidate;
			}
		}
		return hex(hue, saturation, 0.1);
	}

	static String hex(double hue, double saturation, double lightness) {
		double c = (1 - Math.abs(2 * lightness - 1)) * saturation;
		double x = c * (1 - Math.abs((hue / 60) % 2 - 1));
		double m = lightness - c / 2;
		double[] rgb = switch ((int) (hue / 60) % 6) {
			case 0 -> new double[] {c, x, 0};
			case 1 -> new double[] {x, c, 0};
			case 2 -> new double[] {0, c, x};
			case 3 -> new double[] {0, x, c};
			case 4 -> new double[] {x, 0, c};
			default -> new double[] {c, 0, x};
		};
		return String.format(Locale.ROOT, "#%02x%02x%02x", Math.round((rgb[0] + m) * 255), Math.round((rgb[1] + m) * 255),
				Math.round((rgb[2] + m) * 255));
	}

}
