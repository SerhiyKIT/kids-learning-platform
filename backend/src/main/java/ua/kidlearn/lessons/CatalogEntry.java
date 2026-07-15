package ua.kidlearn.lessons;

import java.util.UUID;

public record CatalogEntry(UUID lessonId, String title, String moduleCode, UUID currentVersionId) {
}
