/**
 * Development-only endpoint for creating a TEACHER or ADMIN login — the only way to reach those
 * cabinets, since public registration ({@code AuthService.register}) always creates a PARENT (no
 * open teacher/admin self-signup, by design). Every bean here is active only under the "dev"
 * Spring profile; this package must never ship to production.
 */
package ua.kidlearn.devauth;
