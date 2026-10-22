package ua.kidlearn.groups;

import java.util.UUID;

/**
 * Deliberately excludes anything about the child's parent (name, email, contact).
 *
 * @param ageBand the lessons' age level that fits the child now — age_3_4 or age_5_6 — so the
 * teacher can pick lessons to match (QA1-033); the level, not the birth year
 */
public record GroupMemberInfo(UUID childId, String displayName, String avatarId, String ageBand) {
}
