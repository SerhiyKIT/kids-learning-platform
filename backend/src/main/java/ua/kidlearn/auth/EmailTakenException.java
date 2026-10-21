package ua.kidlearn.auth;

/**
 * The email already belongs to some account (any role, deleted or not). Deliberately carries no
 * detail about that account, so an admin invite can't be used to probe who is registered as what.
 */
public class EmailTakenException extends RuntimeException {

	public EmailTakenException() {
		super("Email already taken");
	}

}
