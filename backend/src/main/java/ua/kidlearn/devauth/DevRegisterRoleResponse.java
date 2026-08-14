package ua.kidlearn.devauth;

import java.util.UUID;
import ua.kidlearn.users.Role;

public record DevRegisterRoleResponse(UUID id, String email, Role role) {
}
