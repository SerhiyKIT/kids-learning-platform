package ua.kidlearn.attempts;

import java.util.List;
import java.util.UUID;

/** Deliberately excludes anything about the child's parent (name, email, contact). */
public record TeacherResultChild(UUID childId, String displayName, List<TeacherResultAttempt> attempts) {
}
