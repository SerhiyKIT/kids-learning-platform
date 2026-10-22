package ua.kidlearn.lessons;

import java.util.UUID;

/** @param ageBand the published version's age level (age_3_4 / age_5_6), shown when assigning (QA1-033) */
public record CatalogEntry(UUID lessonId, String title, String moduleCode, UUID currentVersionId, String ageBand) {
}
