package ua.kidlearn.auth;

import java.util.UUID;

public record RegisterResponse(UUID id, String email, String displayName) {
}
