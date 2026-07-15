package ua.kidlearn.auth;

import ua.kidlearn.users.User;

record EmailVerificationRequestedEvent(User user, String rawToken) {
}
