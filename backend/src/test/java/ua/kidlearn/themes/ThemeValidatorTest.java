package ua.kidlearn.themes;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ThemeValidatorTest {

	private final ThemeValidator validator = new ThemeValidator();
	private final StubThemeGenerator stub = new StubThemeGenerator();

	private Map<String, String> validPalette() {
		return new LinkedHashMap<>(stub.generate("blue", "space", "calm").palette());
	}

	@Test
	void everyStubThemeForAll72AnswerCombinationsPasses() {
		int combinations = 0;
		for (String color : ThemeCatalog.COLOR_IDS) {
			for (String world : ThemeCatalog.WORLD_IDS) {
				for (String mood : ThemeCatalog.MOOD_IDS) {
					GeneratedTheme theme = stub.generate(color, world, mood);
					ThemeValidator.Result result = validator.validate(theme.palette(), world, theme.guideAccessory(),
							theme.name());
					assertThat(result.problems()).as(color + "|" + world + "|" + mood).isEmpty();
					combinations++;
				}
			}
		}
		assertThat(combinations).isEqualTo(72);
	}

	@Test
	void stubIsDeterministic() {
		assertThat(stub.generate("red", "sea", "bright")).isEqualTo(stub.generate("red", "sea", "bright"));
		assertThat(stub.generate("red", "sea", "bright").palette()).isNotEqualTo(stub.generate("red", "sea", "calm").palette());
	}

	@Test
	void missingExtraAndMalformedTokensAreRejected() {
		Map<String, String> palette = validPalette();
		palette.remove("ink");
		palette.put("background-image", "#ffffff");
		palette.put("sun", "url(evil.png)");
		ThemeValidator.Result result = validator.validate(palette, "space", "helmet", "Test");
		assertThat(result.problems()).contains("palette.ink: missing", "palette.background-image: not a theme token",
				"palette.sun: 'url(evil.png)' is not #RRGGBB");
		assertThat(result.isValid()).isFalse();
	}

	@Test
	void lowInkContrastIsRejected() {
		Map<String, String> palette = validPalette();
		palette.put("ink", "#9a9a9a");
		assertThat(validator.validate(palette, "space", "helmet", "Test").problems())
				.anyMatch(p -> p.startsWith("contrast ink on canvas"));
	}

	@Test
	void interactiveColoursNeedThreeToOneOnEveryBackground() {
		Map<String, String> palette = validPalette();
		palette.put("teal", "#9fd9d1");
		ThemeValidator.Result result = validator.validate(palette, "space", "helmet", "Test");
		assertThat(result.problems()).anyMatch(p -> p.startsWith("contrast teal on bg-top"));
		assertThat(result.contrast()).hasSize(10);
	}

	@Test
	void acidColoursAndDarkBackgroundsAreRejected() {
		Map<String, String> palette = validPalette();
		palette.put("glow", "#00ff00");
		palette.put("pip", "#ff00ff");
		palette.put("bg-bottom", "#3a3a6a");
		ThemeValidator.Result result = validator.validate(palette, "space", "helmet", "Test");
		assertThat(result.problems()).anyMatch(p -> p.startsWith("palette.glow: too saturated"))
				.anyMatch(p -> p.startsWith("palette.pip: too saturated"))
				.anyMatch(p -> p.startsWith("palette.bg-bottom: background too dark"));
	}

	@Test
	void worldAndAccessoryMustComeFromTheCatalog() {
		ThemeValidator.Result result = validator.validate(validPalette(), "jungle", "sunglasses", " ");
		assertThat(result.problems()).contains("worldId: unknown 'jungle'", "guideAccessory: unknown 'sunglasses'",
				"name: must be 1–80 characters");
	}

	/**
	 * Documents a known gap rather than hiding it: today's hand-made theme_default (seeded by V9 with
	 * the current child-mode colours) does not meet the 3:1 rule for the yellow «послухати» and the
	 * teal buttons on the cream background. It isn't generated, so it isn't validated; whether to
	 * retune it is a design decision.
	 */
	@Test
	void themeDefaultFailsOnlyTheInteractiveContrastRule() {
		Map<String, String> defaults = new LinkedHashMap<>();
		defaults.put("canvas", "#ffe9c9");
		defaults.put("bg-top", "#fff3dd");
		defaults.put("bg-bottom", "#ffd39c");
		defaults.put("sun", "#ffc13b");
		defaults.put("sun-deep", "#7a4e0b");
		defaults.put("teal", "#2fb3a3");
		defaults.put("ink", "#5a4322");
		defaults.put("ink-soft", "#8a6a3c");
		defaults.put("pip", "#e8d3ae");
		defaults.put("glow", "#ffd98a");
		defaults.put("correct", "#8ede9f");
		defaults.put("correct-surface", "#eafaee");
		defaults.put("scroll", "#f0c78a");
		defaults.put("guide-main", "#b9e7dd");
		defaults.put("guide-accent", "#a9dfd4");
		assertThat(validator.validate(defaults, "space", "none", "theme_default").problems())
				.isNotEmpty()
				.allMatch(p -> p.startsWith("contrast sun on") || p.startsWith("contrast teal on"));
	}

	@Test
	void contrastMathMatchesWcag() {
		assertThat(ThemeValidator.contrastRatio("#000000", "#ffffff")).isCloseTo(21.0, org.assertj.core.data.Offset.offset(0.01));
		assertThat(ThemeValidator.contrastRatio("#777777", "#ffffff")).isCloseTo(4.48, org.assertj.core.data.Offset.offset(0.01));
	}

}
