"use client";

import { usePathname, useRouter } from "next/navigation";
import { useCallback } from "react";
import { ApiError, statusMessage } from "@/lib/api";

/**
 * One place that turns an API failure into what an adult reads: Ukrainian text chosen by the
 * backend's error code, then by HTTP status. The response's own status text ("Not Found",
 * "Unauthorized") and the server's free-form detail are never shown (QA1-017).
 */
const BY_CODE: Record<string, string> = {
  GROUP_INACTIVE: "Ця група вже неактивна.",
  CHILD_NOT_ACTIVE: "Спершу надайте згоду на профіль дитини.",
  CHILD_RESTING: "Дитина зараз відпочиває.",
  EMAIL_NOT_VERIFIED: "Спершу підтвердьте email — посилання в листі після реєстрації.",
  EMAIL_TAKEN: "Ця адреса вже використовується.",
  STYLE_CONSENT_REQUIRED: "Спершу надайте згоду на персоналізацію оформлення.",
  INVITE_ALREADY_ACCEPTED: "Запрошення вже прийнято.",
  INVALID_CODE: "Невірний код.",
  BAD_CREDENTIALS: "Невірний пароль.",
  NOT_APPROVED: "Спершу схваліть версію.",
  NOT_VALIDATED: "Версія не пройшла автоматичну перевірку.",
  NOT_PUBLISHED: "Версію ще не опубліковано.",
  VOICING_REQUIRES_APPROVED: "Озвучувати можна лише схвалену або опубліковану версію.",
  DELETION_VIA_ADMIN: "Видалити акаунт вчителя може адміністратор.",
  ADMIN_NOT_SELF_DELETABLE: "Акаунт адміністратора не видаляється з кабінету.",
  TWO_FACTOR_SETUP_REQUIRED: "Потрібно налаштувати двофакторний вхід.",
  TWO_FACTOR_VERIFICATION_REQUIRED: "Потрібно підтвердити вхід кодом.",
};

/** What to show the adult for `err`; `overrides` (by code or status) win over the defaults. */
export function errorText(err: unknown, overrides: Record<string | number, string> = {}): string {
  if (!(err instanceof ApiError)) return statusMessage(0);
  if (err.code && overrides[err.code]) return overrides[err.code];
  if (overrides[err.status]) return overrides[err.status];
  if (err.status === 429) {
    return err.retryAfterSeconds
      ? `Занадто багато спроб. Спробуйте за ${err.retryAfterSeconds} с.`
      : "Занадто багато спроб. Спробуйте пізніше.";
  }
  if (err.problems && err.problems.length > 0) return err.problems.map((p) => p.message).join(" ");
  if (err.code && BY_CODE[err.code]) return BY_CODE[err.code];
  return statusMessage(err.status);
}

/**
 * The page to return to after login, if `next` is one of ours: a same-origin path, never a
 * protocol-relative "//host", a backslash trick or a full URL (no open redirect).
 */
export function safeNext(next: string | null | undefined): string | null {
  if (!next || !next.startsWith("/") || next.startsWith("//") || next.includes("\\")) return null;
  try {
    const url = new URL(next, "http://same.origin");
    return url.origin === "http://same.origin" && !url.pathname.startsWith("/login") ? url.pathname + url.search : null;
  } catch {
    return null;
  }
}

export function loginHref(returnTo: string): string {
  const next = safeNext(returnTo);
  return next ? `/login?next=${encodeURIComponent(next)}` : "/login";
}

/**
 * For cabinet pages: a 401 (the session is gone — expired, signed out everywhere, account
 * deactivated) goes straight to the login page, which brings the adult back here afterwards;
 * any other failure comes back as the ApiError to show with errorText. Wrong-password 401s of
 * re-authentication (code BAD_CREDENTIALS) are a form error, not a lost session.
 */
export function useApiErrors(): (err: unknown) => ApiError | null {
  const router = useRouter();
  const pathname = usePathname();
  return useCallback(
    (err: unknown) => {
      if (err instanceof ApiError && err.status === 401 && err.code !== "BAD_CREDENTIALS") {
        router.replace(loginHref(pathname + window.location.search));
        return null;
      }
      return err instanceof ApiError ? err : new ApiError(0, errorText(err));
    },
    [router, pathname],
  );
}
