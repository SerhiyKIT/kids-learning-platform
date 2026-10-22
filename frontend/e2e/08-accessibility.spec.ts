import { expect, test } from "@playwright/test";
import { adminPage } from "./support/admin";
import { expectNoSeriousA11yViolations } from "./support/axe";
import { teacherGroupWithLesson } from "./support/content";
import { parentWithChild } from "./support/users";

test("accessibility: no serious/critical axe violations on the key adult pages", async ({ browser, page }) => {
  await page.goto("/login");
  await expectNoSeriousA11yViolations(page);
  await page.goto("/register");
  await expectNoSeriousA11yViolations(page);
  // QA1-012: sign-up says "you agree to the terms" — the terms exist (a draft for a lawyer).
  await page.getByRole("link", { name: "умовами використання" }).click();
  await expect(page).toHaveURL(/\/terms$/);
  await expect(page.getByRole("heading", { name: "Умови використання", level: 1 })).toBeVisible();
  await expect(page.getByText("ЧЕРНЕТКА — потребує перевірки юристом")).toBeVisible();
  await expectNoSeriousA11yViolations(page);
  await page.getByRole("link", { name: "Політиці конфіденційності" }).click();
  await expect(page).toHaveURL(/\/privacy$/);

  const parent = await parentWithChild(browser, "Ліна");
  await parent.page.goto("/dashboard");
  await expect(parent.page.getByText("Ліна")).toBeVisible();
  await expectNoSeriousA11yViolations(parent.page);
  await parent.page.goto(`/children/${parent.childId}`);
  await expect(parent.page.getByRole("heading", { name: "Ліна" })).toBeVisible();
  await expectNoSeriousA11yViolations(parent.page);
  await parent.page.goto("/account");
  await expect(parent.page.getByRole("heading", { name: "Мій акаунт" })).toBeVisible();
  await expectNoSeriousA11yViolations(parent.page);

  // QA1-035: a modal dialog by keyboard — focus moves in, Tab and Shift+Tab never leave it, the
  // page behind is inert, Esc closes it and focus returns to the button that opened it.
  const opener = parent.page.getByRole("button", { name: "Вийти на всіх пристроях" });
  await opener.focus();
  await parent.page.keyboard.press("Enter");
  const dialog = parent.page.getByRole("dialog");
  await expect(dialog).toBeVisible();
  const focusInDialog = () => parent.page.evaluate(() => Boolean(document.activeElement?.closest("[role=dialog]")));
  expect(await focusInDialog()).toBe(true);
  for (const key of ["Tab", "Tab", "Tab", "Tab", "Shift+Tab", "Shift+Tab", "Shift+Tab", "Shift+Tab", "Shift+Tab"]) {
    await parent.page.keyboard.press(key);
    expect(await focusInDialog(), `focus after ${key}`).toBe(true);
  }
  expect(await parent.page.locator("main").evaluate((main) => main.closest("[inert]") !== null)).toBe(true);
  await expectNoSeriousA11yViolations(parent.page);
  await parent.page.keyboard.press("Escape");
  await expect(dialog).toHaveCount(0);
  await expect(opener).toBeFocused();

  const group = await teacherGroupWithLesson(browser);
  await group.teacher.goto("/groups");
  await expect(group.teacher.getByText("Зайчики").first()).toBeVisible();
  await expectNoSeriousA11yViolations(group.teacher);
  await group.teacher.goto(`/groups/${group.groupId}`);
  await expect(group.teacher.getByRole("heading", { name: "Зайчики" })).toBeVisible();
  await expectNoSeriousA11yViolations(group.teacher);

  const { page: admin, context } = await adminPage(browser);
  await admin.goto("/admin");
  await expect(admin.getByRole("heading", { name: "Адмін-кабінет" })).toBeVisible();
  await expectNoSeriousA11yViolations(admin);
  await admin.goto("/admin/lessons");
  await expect(admin.getByRole("heading", { name: "Уроки", exact: true })).toBeVisible();
  await expectNoSeriousA11yViolations(admin);

  await context.close();
  await group.close();
  await parent.context.close();
});
