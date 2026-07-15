package ua.kidlearn.auth;

import ua.kidlearn.users.User;

record PasswordResetRequestedEvent(User user, String rawToken) {
}
