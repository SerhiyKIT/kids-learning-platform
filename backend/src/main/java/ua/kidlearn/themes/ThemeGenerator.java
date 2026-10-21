package ua.kidlearn.themes;

/**
 * Picks a palette (the catalog's tokens), a guide accessory and a name for one combination of
 * answers. Receives ONLY catalog ids — never anything about a child. Exactly one implementation
 * is wired: AnthropicThemeGenerator behind the same switch as the lesson LLM, otherwise the
 * deterministic StubThemeGenerator (dev, tests, CI).
 */
public interface ThemeGenerator {

	GeneratedTheme generate(String colorId, String worldId, String moodId);

}
