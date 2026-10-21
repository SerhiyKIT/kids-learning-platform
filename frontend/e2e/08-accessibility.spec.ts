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
