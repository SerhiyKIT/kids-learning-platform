package ua.kidlearn.themes;

import java.util.UUID;
import tools.jackson.databind.JsonNode;

/**
 * GET/PUT /api/children/{id}/style — what child mode applies and what the parent sees.
 *
 * @param answers the child's picks, or null (none yet, or no consent)
 * @param theme always an APPROVED theme: the child's own once approved, otherwise theme_default
 * @param pending the answers' theme isn't approved yet (the guide is "still drawing")
 * @param announce the child's approved theme changed and the guide hasn't announced it yet
 */
public record StyleResponse(boolean consented, StyleAnswers answers, ThemeView theme, boolean pending,
		boolean announce) {

	/** Only the tokens child mode may apply — nothing else about the theme. */
	public record ThemeView(UUID id, JsonNode palette, String worldId, String guideAccessory) {
	}

}
