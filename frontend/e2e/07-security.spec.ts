import { expect, test } from "@playwright/test";
import { api, openLogin } from "./support/api";
import { parentWithChild } from "./support/users";

test("security: anonymous → login; another parent's child → not found; dev endpoints don't exist", async ({ browser, page }) => {
  await page.goto("/dashboard");
  await expect(page).toHaveURL(/\/login/);

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

  await owner.context.close();
  await stranger.context.close();
});
