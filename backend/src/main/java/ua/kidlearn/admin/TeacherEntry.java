package ua.kidlearn.admin;

import java.time.Instant;
import java.util.UUID;

/**
 * One teacher in the admin teacher list. Deliberately excludes any child or parent data — only
 * the teacher's own account fields and how many groups they own.
 *
 * @param status "invited" (invitation not accepted yet), "active", or "deactivated"
 */
public record TeacherEntry(UUID id, String email, String displayName, String status, long groupCount,
		Instant createdAt) {
}
