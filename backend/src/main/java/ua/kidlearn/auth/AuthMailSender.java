package ua.kidlearn.auth;

import ua.kidlearn.users.User;

public interface AuthMailSender {

	void sendVerification(User user, String rawToken);

	void sendPasswordReset(User user, String rawToken);

	void sendTeacherInvite(User user, String rawToken);

	/** To the owner of an existing account someone just tried to sign up with: log in or reset. */
	void sendAlreadyRegistered(User user);

}
