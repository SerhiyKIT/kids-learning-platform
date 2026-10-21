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

	private EmailAddress() {
	}

	public static String normalize(String email) {
		return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
	}

}
