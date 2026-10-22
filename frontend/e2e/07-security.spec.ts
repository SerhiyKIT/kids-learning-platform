import { expect, test } from "@playwright/test";
import { api, openLogin } from "./support/api";
import { PASSWORD, parentWithChild } from "./support/users";

test("security: anonymous → login; another parent's child → not found; dev endpoints don't exist", async ({ browser, page }) => {
  // QA1-037: opening the login page costs no failing request (the CSRF warm-up used to 404).
  const failures: string[] = [];
  page.on("response", (r) => {
    if (r.status() >= 400) failures.push(`${r.status()} ${new URL(r.url()).pathname}`);
  });
  await page.goto("/dashboard");
  await expect(page).toHaveURL(/\/login/);
  await page.waitForFunction(() => document.cookie.includes("XSRF-TOKEN="));
  expect(failures.filter((f) => !f.endsWith("/api/auth/me"))).toEqual([]);

  const owner = await parentWithChild(browser, "Чужа дитина");
  const stranger = await parentWithChild(browser, "Своя дитина");
  await stranger.page.goto(`/children/${owner.childId}`);
  await expect(stranger.page.getByText("Дитину не знайдено.")).toBeVisible();
  expect((await api(stranger.page, "GET", `/children/${owner.childId}/export`)).status).toBe(404);

  expect((await api(stranger.page, "POST", "/dev/seed-demo")).status).toBe(404);
  expect((await api(stranger.page, "POST", "/dev/register-role", {
    email: "x@example.test", password: "e2e-password-123", displayName: "X", role: "ADMIN",
  })).status).toBe(404);
  await openLogin(page);
  expect((await api(page, "GET", "/actuator/env")).status).not.toBe(200);
  // QA1-025: none of create-next-app's sample files are served.
  for (const sample of ["vercel.svg", "next.svg", "globe.svg", "window.svg", "file.svg"]) {
    expect((await page.request.get(`/${sample}`)).status(), sample).toBe(404);
  }

  // QA1-017: the session ends under an open cabinet page (signed out everywhere from elsewhere).
  // The next action leads to the login page, which brings the parent back — never a raw
  // "Unauthorized" on the page.
  const childPage = `/children/${owner.childId}`;
  await owner.page.goto(childPage);
  await expect(owner.page.getByRole("radio", { name: "20 хв" })).toBeVisible();
  await api(owner.page, "POST", "/me/logout-everywhere");
  await owner.page.getByRole("radio", { name: "20 хв" }).click();
  await expect(owner.page).toHaveURL(new RegExp(`/login\\?next=${encodeURIComponent(childPage)}$`));
  await expect(owner.page.getByText(/Unauthorized|Not Found|Bad Request/)).toHaveCount(0);
  await owner.page.waitForFunction(() => document.cookie.includes("XSRF-TOKEN="));
  await owner.page.getByLabel("Електронна пошта").fill(owner.email);
  await owner.page.locator("#password").fill(PASSWORD);
  await owner.page.getByRole("button", { name: "Увійти" }).click();
  await expect(owner.page).toHaveURL(new RegExp(`${childPage}$`));

  // …but only to a page of ours: no open redirect through ?next=.
  for (const next of ["//evil.example/x", "https://evil.example/x", "/\\evil.example"]) {
    await stranger.page.goto(`/login?next=${encodeURIComponent(next)}`);
    await stranger.page.waitForFunction(() => document.cookie.includes("XSRF-TOKEN="));
    await stranger.page.getByLabel("Електронна пошта").fill(stranger.email);
    await stranger.page.locator("#password").fill(PASSWORD);
    await stranger.page.getByRole("button", { name: "Увійти" }).click();
    await expect(stranger.page).toHaveURL(/\/dashboard$/);
    expect(new URL(stranger.page.url()).hostname).toBe("127.0.0.1");
  }

  await owner.context.close();
  await stranger.context.close();
});
