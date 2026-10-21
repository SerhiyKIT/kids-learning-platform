import { expect, test } from "@playwright/test";
import { api } from "./support/api";
import { teacherGroupWithLesson } from "./support/content";
import { exitChildMode, playStubLesson } from "./support/kid-play";
import { PASSWORD, parentWithChild } from "./support/users";

// The e2e profile makes one "minute" of the limit last a second (app.kid.limit-unit) and the rest 2 minutes.
test("session limit: parent sets it → after the lesson the child sleeps → reload keeps it → parent wakes the child @phone", async ({ browser, isMobile }) => {
  const group = await teacherGroupWithLesson(browser);
  const parent = await parentWithChild(browser, "Соня");
  const page = parent.page;
  expect((await api(page, "POST", "/groups/join", { joinCode: group.joinCode, childId: parent.childId })).status).toBe(200);

  await page.goto(`/children/${parent.childId}`);
  await page.getByRole("radio", { name: "10 хв" }).click();
  await expect(page.getByRole("radio", { name: "10 хв" })).toHaveAttribute("aria-checked", "true");

  await page.goto(`/play/${parent.childId}`);
  await page.getByRole("button", { name: "Почнімо!" }).first().click();
  await page.locator(".kid-shelf a").first().click();
  const started = Date.now();
  await playStubLesson(page, isMobile, async () => {
    // The limit applies only when the lesson ends — make sure 10 "minutes" (s) have passed by then.
    await page.waitForTimeout(Math.max(0, 11_000 - (Date.now() - started)));
  });

  await expect(page.locator('[data-phase="farewell"]')).toBeVisible({ timeout: 30_000 });
  await expect(page.locator('[data-sleeping="1"]')).toBeVisible({ timeout: 30_000 });
  expect((await page.locator("body").innerText()).replace(/z/g, "").trim()).toBe("");

  await page.reload();
  await page.getByRole("button", { name: "Почнімо!" }).first().click();
  await expect(page.locator('[data-sleeping="1"]')).toBeVisible();

  await exitChildMode(page, PASSWORD);
  await expect(page.getByText(/Відпочиває до \d{2}:\d{2}/)).toBeVisible();
  await page.getByRole("button", { name: "Дозволити грати зараз" }).click();
  await expect(page.getByText(/Відпочиває до/)).toHaveCount(0);

  await page.goto(`/play/${parent.childId}`);
  await page.getByRole("button", { name: "Почнімо!" }).first().click();
  await expect(page.locator(".kid-shelf a")).toHaveCount(1);

  await group.close();
  await parent.context.close();
});
