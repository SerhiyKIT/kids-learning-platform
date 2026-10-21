package ua.kidlearn.account;

/** Teachers and admins can't delete themselves (an admin must do it / the last admin must stay). */
public class AccountDeletionNotAllowedException extends RuntimeException {

	private final String code;

	public AccountDeletionNotAllowedException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String getCode() {
		return code;
	}

}
