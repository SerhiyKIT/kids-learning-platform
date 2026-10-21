package ua.kidlearn.themes;

import java.util.UUID;
import ua.kidlearn.users.Role;

/** Published when a child's answers need a theme that doesn't exist yet; handled after commit. */
record ThemeGenerationRequested(String preferenceKey, UUID actorId, Role actorRole) {
}
