// Admin 2FA gate (ua.kidlearn.twofa.TwoFactorGateFilter): until an admin session is elevated,
// every other admin request (and, while verification is pending, even /auth/me) answers
// 403 {"code":"TWO_FACTOR_SETUP_REQUIRED" | "TWO_FACTOR_VERIFICATION_REQUIRED"}. Pages pass any
// caught error through twoFactorRedirect and router.replace() to the returned screen instead of
// showing the 403 — the transport (api.ts) deliberately knows nothing about this.

import { ApiError } from "./api";

export const TWO_FACTOR_SETUP_PATH = "/admin/2fa/setup";
export const TWO_FACTOR_VERIFY_PATH = "/admin/2fa/verify";

/** POST /api/admin/2fa/setup */
export interface TwoFactorSetupResponse {
  otpauthUri: string;
  secretBase32: string;
}

/** POST /api/admin/2fa/enable — the plaintext codes are returned exactly once. */
export interface TwoFactorEnableResponse {
  backupCodes: string[];
}

/** Where a 2FA-gated admin session has to go next, or null if `err` isn't a 2FA gate response. */
export function twoFactorRedirect(err: unknown): string | null {
  if (!(err instanceof ApiError) || err.status !== 403) return null;
  if (err.code === "TWO_FACTOR_SETUP_REQUIRED") return TWO_FACTOR_SETUP_PATH;
  if (err.code === "TWO_FACTOR_VERIFICATION_REQUIRED") return TWO_FACTOR_VERIFY_PATH;
  return null;
}

/** User-facing text for a failed POST /admin/2fa/enable or /admin/2fa/verify. */
export function codeErrorText(err: unknown): string {
  if (err instanceof ApiError && err.status === 400) return "Невірний код. Перевірте його та спробуйте ще раз.";
  if (err instanceof ApiError && err.status === 429) {
    return err.retryAfterSeconds
      ? `Занадто багато спроб. Спробуйте за ${err.retryAfterSeconds} с.`
      : "Занадто багато спроб. Спробуйте пізніше.";
  }
  if (err instanceof ApiError && err.status === 401) return "Сесія завершилась. Увійдіть знову.";
  return "Несподівана помилка. Спробуйте ще раз.";
}
