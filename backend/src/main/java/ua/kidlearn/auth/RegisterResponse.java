package ua.kidlearn.auth;

/**
 * The one answer to every sign-up (202), whether or not the address already has an account
 * (QA1-006): what happened is told only to the mailbox's owner.
 */
public record RegisterResponse(String status) {

	public static final RegisterResponse CHECK_EMAIL = new RegisterResponse("check_email");

}
