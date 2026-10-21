package ua.kidlearn.themes;

import jakarta.validation.constraints.NotNull;

/** The child's three picks — catalog ids only (validated against ThemeCatalog), never free text. */
public record StyleAnswers(@NotNull String colorId, @NotNull String worldId, @NotNull String moodId) {

	public String preferenceKey() {
		return ThemeCatalog.preferenceKey(colorId, worldId, moodId);
	}

}
