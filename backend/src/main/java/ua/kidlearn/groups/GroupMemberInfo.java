package ua.kidlearn.groups;

import java.util.UUID;

/** Deliberately excludes anything about the child's parent (name, email, contact). */
public record GroupMemberInfo(UUID childId, String displayName, String avatarId) {
}
