package ua.kidlearn.auth;

import java.util.UUID;
import ua.kidlearn.users.Role;

public record MeResponse(UUID id, String email, Role role, boolean emailVerified, String displayName) {
}
