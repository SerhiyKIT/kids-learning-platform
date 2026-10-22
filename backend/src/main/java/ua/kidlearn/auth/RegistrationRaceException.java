package ua.kidlearn.auth;

/**
 * A concurrent sign-up took the address between AuthService#register's check and its insert. The
 * transaction is rolled back; AuthController answers exactly as for any other sign-up.
 */
class RegistrationRaceException extends RuntimeException {

	RegistrationRaceException(Throwable cause) {
		super("Email taken by a concurrent registration", cause);
	}

}
