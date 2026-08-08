// API client for the session-cookie-based backend. Auth lives in the httpOnly session cookie —
// this file never touches localStorage; it only echoes the readable XSRF-TOKEN cookie back as a
// header, per Spring Security's CookieCsrfTokenRepository.withHttpOnlyFalse() SPA setup.

const API_BASE = "/api";
const CSRF_COOKIE_NAME = "XSRF-TOKEN";
const CSRF_HEADER_NAME = "X-XSRF-TOKEN";

export interface ApiProblem {
  path: string;
  message: string;
}

export class ApiError extends Error {
  status: number;
  code?: string;
  problems?: ApiProblem[];
  retryAfterSeconds?: number;

  constructor(
    status: number,
    message: string,
    opts?: { code?: string; problems?: ApiProblem[]; retryAfterSeconds?: number },
  ) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = opts?.code;
    this.problems = opts?.problems;
    this.retryAfterSeconds = opts?.retryAfterSeconds;
  }
}

function readCookie(name: string): string | null {
  if (typeof document === "undefined") return null;
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`));
  return match ? decodeURIComponent(match[1]) : null;
}

/**
 * Fire-and-forget GET that provisions the XSRF-TOKEN cookie before the first state-changing
 * request — the backend only sets it as a side effect of a response passing through its filter
 * chain, so a brand-new session has no cookie yet to echo back. Deliberately targets GET /login
 * (public, and already proxied for AJAX calls — see next.config.ts) rather than an
 * authentication-requiring endpoint like /api/auth/me: an anonymous request to a protected
 * endpoint gets captured by Spring Security's SavedRequest cache, which would then hijack a
 * later successful login's redirect target away from this app.
 */
export function ensureCsrfCookie(): void {
  void fetch("/login", { credentials: "include", headers: { "X-Requested-With": "XMLHttpRequest" } }).catch(() => {});
}

async function toApiError(response: Response): Promise<ApiError> {
  const retryAfterHeader = response.headers.get("Retry-After");
  const retryAfterSeconds = retryAfterHeader ? Number(retryAfterHeader) : undefined;
  let message = response.statusText || `Request failed with status ${response.status}`;
  let code: string | undefined;
  let problems: ApiProblem[] | undefined;
  try {
    const data: unknown = await response.json();
    if (data && typeof data === "object") {
      const record = data as Record<string, unknown>;
      if (typeof record.detail === "string") message = record.detail;
      if (typeof record.code === "string") code = record.code;
      if (Array.isArray(record.problems)) problems = record.problems as ApiProblem[];
    }
  } catch {
    // No JSON body (e.g. a bare 401/403) — keep the status-derived message.
  }
  return new ApiError(response.status, message, { code, problems, retryAfterSeconds });
}

/** POSTs/PUTs/etc. under /api/*, with the session cookie and (for non-GET) the CSRF header. */
export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? "GET").toUpperCase();
  const headers = new Headers(init.headers);
  if (method !== "GET" && method !== "HEAD") {
    const csrfToken = readCookie(CSRF_COOKIE_NAME);
    if (csrfToken) headers.set(CSRF_HEADER_NAME, csrfToken);
  }
  if (init.body !== undefined && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(`${API_BASE}${path}`, { ...init, method, headers, credentials: "include" });

  if (!response.ok) {
    throw await toApiError(response);
  }
  if (response.status === 204 || response.headers.get("Content-Length") === "0") {
    return undefined as T;
  }
  return (await response.json()) as T;
}

/**
 * Spring Security's form-login endpoint responds with a redirect, not JSON, so this doesn't
 * reuse apiFetch directly. That redirect's Location is an ABSOLUTE backend-origin URL (Tomcat
 * has no idea it's sitting behind the dev proxy), so letting fetch() auto-follow it would make
 * the browser issue a second, genuinely cross-origin request straight to BACKEND_ORIGIN — which
 * has no CORS headers and would just fail. `redirect: "manual"` stops fetch from ever attempting
 * that hop; we then ask GET /api/auth/me (same-origin, ordinary apiFetch) whether the session is
 * now authenticated, which is what we actually care about rather than the redirect target.
 */
export async function loginWithFormPost(email: string, password: string): Promise<void> {
  const csrfToken = readCookie(CSRF_COOKIE_NAME);
  const body = new URLSearchParams({ username: email, password });
  const response = await fetch("/login", {
    method: "POST",
    credentials: "include",
    redirect: "manual",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
      // Tells next.config.ts's beforeFiles rewrite this is our AJAX call, not a browser
      // navigation, so it (and only it) gets routed to the backend instead of our /login page.
      "X-Requested-With": "XMLHttpRequest",
      ...(csrfToken ? { [CSRF_HEADER_NAME]: csrfToken } : {}),
    },
    body,
  });

  if (response.status === 429) {
    throw await toApiError(response);
  }
  try {
    await apiFetch("/auth/me");
  } catch (err) {
    if (err instanceof ApiError && err.status === 401) {
      throw new ApiError(401, "Невірний email або пароль");
    }
    throw err;
  }
}

/** Same cross-origin-redirect concern as loginWithFormPost; the result doesn't matter here. */
export async function logout(): Promise<void> {
  const csrfToken = readCookie(CSRF_COOKIE_NAME);
  await fetch("/logout", {
    method: "POST",
    credentials: "include",
    redirect: "manual",
    headers: {
      "X-Requested-With": "XMLHttpRequest",
      ...(csrfToken ? { [CSRF_HEADER_NAME]: csrfToken } : {}),
    },
  });
}
