package ua.kidlearn.auth;

import ua.kidlearn.users.User;

public interface AuthMailSender {

	void sendVerification(User user, String rawToken);

	void sendPasswordReset(User user, String rawToken);

}
