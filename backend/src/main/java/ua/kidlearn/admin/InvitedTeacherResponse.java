package ua.kidlearn.admin;

import java.util.UUID;

/** POST /api/admin/teachers result — never contains a password or the invitation token. */
public record InvitedTeacherResponse(UUID id, String email, String displayName, String status) {
}
