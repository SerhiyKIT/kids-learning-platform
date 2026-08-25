package ua.kidlearn.bootstrap;

import java.util.UUID;
import ua.kidlearn.users.Role;

public record BootstrapAdminResponse(UUID id, String email, Role role) {
}
