package ua.kidlearn.attempts;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The child's sticker album (docs/UX_гайд_дитячого_режиму.md §7). Deliberately no score or any
 * other quality grade: a passed lesson simply earns its sticker.
 *
 * @param stickers one per lesson with at least one completed attempt, earliest first
 * @param topicsCompleted topics whose every published lesson assigned to the child has a sticker
 * @param locked assigned published lessons without a sticker yet — the album's grey silhouettes
 */
public record StickersResponse(List<Sticker> stickers, List<String> topicsCompleted, List<LockedSticker> locked) {

	/**
	 * @param lessonVersionId the version of the first completed attempt — the bridge comes from it
	 * @param earnedAt that attempt's completion time
	 */
	public record Sticker(UUID lessonId, UUID lessonVersionId, String title, String topic, Instant earnedAt,
			Bridge bridge) {
	}

	/** The lesson's reality_bridge line; audioUrl is null until it has been voiced. */
	public record Bridge(String key, String text, String audioUrl) {
	}

	public record LockedSticker(UUID lessonId, String topic) {
	}

}
