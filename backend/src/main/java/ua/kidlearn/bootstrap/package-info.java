/**
 * One-time, token-gated creation of the very first ADMIN account, for use in production where
 * {@code ua.kidlearn.devauth} (dev-profile only) isn't available. Safety comes from three things
 * together, not a Spring profile: {@link ua.kidlearn.bootstrap.BootstrapProperties}'s token must
 * be configured, no ADMIN may already exist, and the caller must present that token (compared in
 * constant time). Once any ADMIN exists the endpoint permanently 410s — it never creates a second
 * one — so the token can be left configured, rotated, or removed afterward without reopening
 * anything.
 */
package ua.kidlearn.bootstrap;
