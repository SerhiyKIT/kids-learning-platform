import { expect, type Browser, type BrowserContext, type BrowserContextOptions, type Page } from "@playwright/test";
import { api, openLogin } from "./api";
import { linkFromEmail } from "./mailpit";

export const PASSWORD = "e2e-password-123";

/** A unique address per test and run, so specs never depend on each other's users. */
export function uniqueEmail(role: string): string {
  return `e2e-${role}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}@example.test`;
}

/** Signs in through the login form and waits until the app has navigated away from it. */
export async function signIn(page: Page, email: string, password = PASSWORD): Promise<void> {
  await openLogin(page);
  await page.getByLabel("Електронна пошта").fill(email);
  await page.locator("#password").fill(password);
  await page.getByRole("button", { name: "Увійти" }).click();
  await expect(page).not.toHaveURL(/\/login/);
}

export interface Parent {
  context: BrowserContext;
  page: Page;
  email: string;
}

/**
 * A verified, signed-in parent with one active child, set up through the public API and the
 * verification email (spec 1 covers the same steps through the UI).
 */
export async function parentWithChild(
  browser: Browser,
  childName = "Тестова Дитина",
  contextOptions: BrowserContextOptions = {},
): Promise<Parent & { childId: string }> {
  const context = await browser.newContext(contextOptions);
  const page = await context.newPage();
  const email = uniqueEmail("parent");
  await openLogin(page);
  const registered = await api(page, "POST", "/auth/register", { email, password: PASSWORD, displayName: "Мама" });
  expect(registered.status).toBe(202);
  const link = await linkFromEmail(email, "/verify-email");
  expect((await api(page, "POST", "/auth/verify-email", { token: link.searchParams.get("token") })).status).toBe(200);
  await signIn(page, email);
  const child = await api<{ id: string }>(page, "POST", "/children", {
    displayName: childName,
    birthYear: new Date().getFullYear() - 5,
    relation: "mother",
  });
  expect(child.status).toBe(201);
  expect((await api(page, "POST", `/children/${child.json.id}/consent`)).status).toBe(200);
  return { context, page, email, childId: child.json.id };
}
