package ua.kidlearn.users;

import jakarta.servlet.http.HttpSession;
import java.time.Instant;

/**
 * When a session was authenticated, kept as a session attribute set at login. Not
 * HttpSession.getCreationTime(): login only changes the id of an already existing session
 * (session-fixation protection), so its creation time can predate the login itself — a session
 * opened before "sign out everywhere" and logged into afterwards would then be wrongly rejected.
 */
public final class SessionAuthentication {

	static final String AUTHENTICATED_AT = "kidlearn.authenticatedAt";

	private SessionAuthentication() {
	}

	public static void markAuthenticated(HttpSession session, Instant at) {
		session.setAttribute(AUTHENTICATED_AT, at);
	}

	/**
	 * A session is stale when its account invalidated sessions after it was authenticated. A
	 * session without the attribute (authenticated before this mechanism existed) is stale as soon
	 * as any invalidation happened.
	 */
	static boolean isStale(HttpSession session, Instant sessionsValidAfter) {
		if (sessionsValidAfter == null) {
			return false;
		}
		Object at = session.getAttribute(AUTHENTICATED_AT);
		return !(at instanceof Instant authenticatedAt) || authenticatedAt.isBefore(sessionsValidAfter);
	}

}
