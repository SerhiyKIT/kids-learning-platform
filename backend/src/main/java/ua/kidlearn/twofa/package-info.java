/**
 * Mandatory TOTP two-factor authentication for ADMIN accounts: setup (generate + confirm a
 * secret, issue one-time backup codes) and the enforced two-step login (a fresh ADMIN session
 * starts "pre-2fa" — either setup-required or verification-pending — and is restricted to a
 * narrow allowlist of paths until it presents a valid TOTP or backup code; see
 * {@link ua.kidlearn.twofa.TwoFactorGateFilter}). No disable endpoint exists: once enabled, 2FA
 * stays mandatory for that admin. Non-admin roles are completely unaffected.
 */
package ua.kidlearn.twofa;
