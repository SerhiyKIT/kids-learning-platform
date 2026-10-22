package ua.kidlearn.users;

import java.util.Locale;

/**
 * The one place an email address is normalized: surrounding whitespace dropped, lower-cased.
 * Applied to every address before it is stored (User's constructor) or looked up
 * (UserRepository's lookups, the login throttle's per-account key), so "Mama@Gmail.com " and
 * "mama@gmail.com" are the same account everywhere. The database enforces the same canonical
 * form (V11).
 */
public final class EmailAddress {

	/**
	 * What every email field accepts on top of {@code @Email}: a domain with a dot (QA1-010) —
	 * {@code @Email} alone lets "a@b" through, an address no mail can reach.
	 */
	public static final String FORMAT = "^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$";

	private EmailAddress() {
	}

	public static String normalize(String email) {
		return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
	}

}
