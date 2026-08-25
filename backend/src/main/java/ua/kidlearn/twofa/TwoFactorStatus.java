package ua.kidlearn.twofa;

/** Per-session ADMIN 2FA gate state — see {@link TwoFactorSession}, {@link TwoFactorGateFilter}. */
enum TwoFactorStatus {

	/** totp_enabled_at is null: only /2fa/setup, /2fa/enable, and /auth/me are reachable. */
	SETUP_REQUIRED,

	/** 2FA is enabled but not yet proven this session: only /2fa/verify is reachable. */
	PENDING_VERIFICATION,

	/** A valid TOTP/backup code was presented this session: full access. */
	ELEVATED

}
