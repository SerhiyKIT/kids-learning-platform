package ua.kidlearn.auth;

/** Wrong password on re-authentication. Carries no detail, like a failed login. */
public class ReauthenticationFailedException extends RuntimeException {

	public ReauthenticationFailedException() {
		super("Re-authentication failed");
	}

}
