package ua.kidlearn.auth;

import ua.kidlearn.users.User;

record TeacherInviteRequestedEvent(User user, String rawToken) {
}
