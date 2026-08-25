package ua.kidlearn.twofa;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** Reads/writes the current session's {@link TwoFactorStatus}. Session-scoped by design: logging
 * out (which invalidates the session) resets it, and a later re-login recomputes it fresh. */
final class TwoFactorSession {

	private static final String ATTRIBUTE = "twofa.status";

	private TwoFactorSession() {
	}

	/** Called by the login success handler right after authentication, for ADMIN users only. */
	static void init(HttpServletRequest request, TwoFactorStatus status) {
		request.getSession(true).setAttribute(ATTRIBUTE, status);
	}

	/** Defaults to the most restrictive state if unset (e.g. a non-formLogin session somehow
	 * reaching the gate) — fail closed, not open. */
	static TwoFactorStatus status(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		Object value = session != null ? session.getAttribute(ATTRIBUTE) : null;
		return value instanceof TwoFactorStatus status ? status : TwoFactorStatus.SETUP_REQUIRED;
	}

	static void elevate(HttpServletRequest request) {
		request.getSession(true).setAttribute(ATTRIBUTE, TwoFactorStatus.ELEVATED);
	}

}
