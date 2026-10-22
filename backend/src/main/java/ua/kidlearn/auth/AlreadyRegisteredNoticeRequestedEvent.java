package ua.kidlearn.auth;

import ua.kidlearn.users.User;

/** Someone signed up with the address of an existing, verified account (see AuthService#register). */
record AlreadyRegisteredNoticeRequestedEvent(User user) {
}
