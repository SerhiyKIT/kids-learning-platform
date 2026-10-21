package ua.kidlearn.themes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Automatic checks a generated theme must pass before an admin ever sees it as a candidate
 * (thresholds documented in docs/Специфікація_ассетів.md "Теми персоналізації"):
 * <ul>
 * <li>exactly the catalog's palette tokens, each a #RRGGBB colour;</li>
 * <li>WCAG 2.x contrast: ink on every stage background ≥ 4.5:1 (text); the interactive colours
 * (sun = «послухати», teal = «далі»/album) on every background ≥ 3:1 (non-text UI), and the icon
 * colour on the sun button ≥ 3:1;</li>
 * <li>"not acidic", in HSL terms: every token's chroma C = (1 − |2L − 1|)·S ≤ {@value #MAX_CHROMA}
 * (rules out pure/neon colours such as #00ff00 or #ff00ff), and the stage backgrounds are light,
 * L ≥ {@value #MIN_BACKGROUND_LIGHTNESS};</li>
 * <li>world and guide accessory from the catalog; a short name.</li>
 * </ul>
 */
@Component
public class ThemeValidator {

	static final double MIN_TEXT_CONTRAST = 4.5;
	static final double MIN_UI_CONTRAST = 3.0;
	static final double MAX_CHROMA = 0.82;
	static final double MIN_BACKGROUND_LIGHTNESS = 0.80;
	private static final int MAX_NAME_LENGTH = 80;
	private static final Pattern HEX = Pattern.compile("^#[0-9a-fA-F]{6}$");

	/** One contrast pair, as shown to the moderator. */
	public record ContrastCheck(String foreground, String background, double ratio, double minimum, boolean passes) {
	}

	public record Result(List<String> problems, List<ContrastCheck> contrast) {

		public boolean isValid() {
			return problems.isEmpty();
		}

	}

	public Result validate(Map<String, String> palette, String worldId, String guideAccessory, String name) {
		List<String> problems = new ArrayList<>();
		if (name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH) {
			problems.add("name: must be 1–" + MAX_NAME_LENGTH + " characters");
		}
		if (!ThemeCatalog.WORLD_IDS.contains(worldId)) {
			problems.add("worldId: unknown '" + worldId + "'");
		}
		if (!ThemeCatalog.GUIDE_ACCESSORIES.contains(guideAccessory)) {
			problems.add("guideAccessory: unknown '" + guideAccessory + "'");
		}
		for (String key : palette.keySet()) {
			if (!ThemeCatalog.PALETTE_TOKENS.contains(key)) {
				problems.add("palette." + key + ": not a theme token");
			}
		}
		boolean allColours = true;
		for (String token : ThemeCatalog.PALETTE_TOKENS) {
			String value = palette.get(token);
			if (value == null) {
				problems.add("palette." + token + ": missing");
				allColours = false;
			} else if (!HEX.matcher(value).matches()) {
				problems.add("palette." + token + ": '" + value + "' is not #RRGGBB");
				allColours = false;
			}
		}
		if (!allColours) {
			return new Result(problems, List.of());
		}

		List<ContrastCheck> contrast = new ArrayList<>();
		for (String background : ThemeCatalog.BACKGROUND_TOKENS) {
			contrast.add(check(palette, "ink", background, MIN_TEXT_CONTRAST));
			contrast.add(check(palette, "sun", background, MIN_UI_CONTRAST));
			contrast.add(check(palette, "teal", background, MIN_UI_CONTRAST));
		}
		contrast.add(check(palette, "sun-deep", "sun", MIN_UI_CONTRAST));
		for (ContrastCheck c : contrast) {
			if (!c.passes()) {
				problems.add(String.format(Locale.ROOT, "contrast %s on %s: %.2f:1 < %.1f:1", c.foreground(),
						c.background(), c.ratio(), c.minimum()));
			}
		}

		for (String token : ThemeCatalog.PALETTE_TOKENS) {
			double[] hsl = hsl(palette.get(token));
			double chroma = (1 - Math.abs(2 * hsl[2] - 1)) * hsl[1];
			if (chroma > MAX_CHROMA) {
				problems.add(String.format(Locale.ROOT, "palette.%s: too saturated (chroma %.2f > %.2f)", token, chroma,
						MAX_CHROMA));
			}
		}
		for (String token : ThemeCatalog.BACKGROUND_TOKENS) {
			double lightness = hsl(palette.get(token))[2];
			if (lightness < MIN_BACKGROUND_LIGHTNESS) {
				problems.add(String.format(Locale.ROOT, "palette.%s: background too dark (L %.2f < %.2f)", token,
						lightness, MIN_BACKGROUND_LIGHTNESS));
			}
		}
		return new Result(problems, contrast);
	}

	private static ContrastCheck check(Map<String, String> palette, String foreground, String background,
			double minimum) {
		double ratio = contrastRatio(palette.get(foreground), palette.get(background));
		// Rounded the way it's shown, so 4.499 never "passes" on screen while failing here.
		double shown = Math.round(ratio * 100) / 100.0;
		return new ContrastCheck(foreground, background, shown, minimum, shown >= minimum);
	}

	/** WCAG 2.x contrast ratio of two #RRGGBB colours. */
	static double contrastRatio(String a, String b) {
		double la = relativeLuminance(a);
		double lb = relativeLuminance(b);
		return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
	}

	static double relativeLuminance(String hex) {
		double[] rgb = rgb(hex);
		double[] linear = new double[3];
		for (int i = 0; i < 3; i++) {
			double c = rgb[i];
			linear[i] = c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
		}
		return 0.2126 * linear[0] + 0.7152 * linear[1] + 0.0722 * linear[2];
	}

	/** [hue 0–360, saturation 0–1, lightness 0–1]. */
	static double[] hsl(String hex) {
		double[] rgb = rgb(hex);
		double max = Math.max(rgb[0], Math.max(rgb[1], rgb[2]));
		double min = Math.min(rgb[0], Math.min(rgb[1], rgb[2]));
		double l = (max + min) / 2;
		double d = max - min;
		if (d == 0) {
			return new double[] {0, 0, l};
		}
		double s = d / (1 - Math.abs(2 * l - 1));
		double h;
		if (max == rgb[0]) {
			h = 60 * (((rgb[1] - rgb[2]) / d) % 6);
		} else if (max == rgb[1]) {
			h = 60 * ((rgb[2] - rgb[0]) / d + 2);
		} else {
			h = 60 * ((rgb[0] - rgb[1]) / d + 4);
		}
		return new double[] {(h + 360) % 360, s, l};
	}

	private static double[] rgb(String hex) {
		int value = Integer.parseInt(hex.substring(1), 16);
		return new double[] {((value >> 16) & 0xff) / 255.0, ((value >> 8) & 0xff) / 255.0, (value & 0xff) / 255.0};
	}

}
