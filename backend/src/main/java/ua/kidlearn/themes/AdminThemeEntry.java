package ua.kidlearn.themes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

/**
 * A theme as the moderator sees it: the answers it is for, its palette with the validator's
 * contrast checks (for the preview), and why it was rejected if it was. No child data — a theme
 * belongs to an answer combination, not to a child.
 */
public record AdminThemeEntry(UUID id, String name, String preferenceKey, String colorId, String worldId,
		String moodId, JsonNode palette, String guideAccessory, String status, String rejectReason, String generatedBy,
		String aiModel, Instant createdAt, Instant approvedAt, List<ThemeValidator.ContrastCheck> contrast,
		List<String> problems) {
}
