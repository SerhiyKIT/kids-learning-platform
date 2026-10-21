package ua.kidlearn.lessons;

import java.util.UUID;

/**
 * One row of the admin lesson list (GET /api/admin/lessons).
 *
 * @param latestVersion highest versionNo of the lesson, whatever its status; null if it has none
 * @param publishedVersionId the version children currently get (lessons.current_version_id), or null
 */
public record AdminLessonEntry(UUID id, String moduleCode, String title, int versionCount,
		LatestVersion latestVersion, UUID publishedVersionId) {

	public record LatestVersion(UUID id, int versionNo, String status) {
	}

}
