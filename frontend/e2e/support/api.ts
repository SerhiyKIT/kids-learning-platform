import type { Page } from "@playwright/test";

export interface ApiResult<T = unknown> {
  status: number;
  json: T;
}

/**
 * Calls the backend from inside the page, exactly like the app does (session cookie + the
 * XSRF-TOKEN cookie echoed as X-XSRF-TOKEN). For setting up data a spec isn't about.
 */
export async function api<T = unknown>(page: Page, method: string, path: string, body?: unknown): Promise<ApiResult<T>> {
  return page.evaluate(
    async ({ method, path, body }) => {
      const csrf = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/);
      const headers: Record<string, string> = { "Content-Type": "application/json" };
      if (csrf && method !== "GET") headers["X-XSRF-TOKEN"] = decodeURIComponent(csrf[1]);
      const response = await fetch(`/api${path}`, {
        method,
        credentials: "include",
        headers,
        body: body === undefined ? undefined : JSON.stringify(body),
      });
      const text = await response.text();
      let json: unknown = null;
      try {
        json = text ? JSON.parse(text) : null;
      } catch {
        json = text;
      }
      return { status: response.status, json };
    },
    { method, path, body },
  ) as Promise<ApiResult<T>>;
}

/** Opens /login and waits for the CSRF cookie the app needs before any POST. */
export async function openLogin(page: Page): Promise<void> {
  await page.goto("/login");
  await page.waitForFunction(() => document.cookie.includes("XSRF-TOKEN="));
}
