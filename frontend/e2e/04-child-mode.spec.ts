import { expect, test } from "@playwright/test";
import { teacherGroupWithLesson } from "./support/content";
import { expectKidTargets } from "./support/kid";
import { exitChildMode, playStubLesson } from "./support/kid-play";
import { PASSWORD, parentWithChild } from "./support/users";

test("child mode: join by code → play choice + sorting → celebration → sticker; teacher sees it; protected exit @phone", async ({ browser, isMobile }) => {
  const group = await teacherGroupWithLesson(browser);
  const parent = await parentWithChild(browser, "Остапчик");
  const page = parent.page;

  await page.goto(`/children/${parent.childId}`);
  await page.getByLabel("Приєднатися за кодом").fill(group.joinCode);
  await page.getByRole("button", { name: "Приєднатися" }).click();
  await expect(page.getByText("Зайчики")).toBeVisible();

  await page.goto(`/play/${parent.childId}`);
  await page.getByRole("button", { name: "Почнімо!" }).first().click();
  await expect(page.locator(".kid-shelf a")).toHaveCount(1);
  await expectKidTargets(page);
  await page.locator(".kid-shelf a").first().click();
  await page.locator('button[aria-label="Далі"]').waitFor();
  await expectKidTargets(page);

  await playStubLesson(page, isMobile, async () => expectKidTargets(page));

  await expect(page.locator('[data-phase="celebrate"]')).toBeVisible();
  await expect(page.locator('[data-sticker-new="1"]')).toBeVisible({ timeout: 20_000 });
  await expect(page).toHaveURL(new RegExp(`/play/${parent.childId}$`), { timeout: 30_000 });
  await page.getByRole("link", { name: "Мої наліпки" }).click();
  await expect(page.locator("[data-sticker]")).toHaveCount(1);
  await expectKidTargets(page);

  await group.teacher.goto(`/groups/${group.groupId}`);
  await expect(group.teacher.getByRole("cell", { name: "Остапчик" }).first()).toBeVisible();
  await expect(group.teacher.getByRole("cell", { name: group.lessonTitle }).first()).toBeVisible();

  // A plain tap on the exit does nothing; holding it opens the adult's password screen.
  await page.getByRole("button", { name: "Вихід для дорослих: утримуйте 2 секунди" }).first().click();
  await expect(page.locator("#kid-exit-password")).toHaveCount(0);
  await exitChildMode(page, PASSWORD);
  await expect(page).toHaveURL(new RegExp(`/children/${parent.childId}$`));

  await group.close();
  await parent.context.close();
});
