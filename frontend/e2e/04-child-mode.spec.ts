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
  // QA1-013: a wrong code gets a Ukrainian explanation; the right one works as typed on a phone —
  // lower case, with stray spaces.
  await page.getByLabel("Приєднатися за кодом").fill("ZZZZZZZZ");
  await page.getByRole("button", { name: "Приєднатися" }).click();
  await expect(page.getByText("Код не знайдено. Перевірте, чи правильно його введено.")).toBeVisible();
  await page.getByLabel("Приєднатися за кодом").fill(`  ${group.joinCode.toLowerCase()} `);
  await page.getByRole("button", { name: "Приєднатися" }).click();
  await expect(page.getByText("Зайчики")).toBeVisible();

  // QA1-023: a phone held — or rotation-locked — in portrait gets «Почнімо!» on the "turn the
  // device" picture itself, with a hint for the adult; turning the device then shows the shelf.
  const landscape = page.viewportSize()!;
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto(`/play/${parent.childId}`);
  await expect(page.getByRole("img", { name: "Поверни пристрій горизонтально" })).toBeVisible();
  await expect(page.getByText("Для дорослого: якщо екран не повертається")).toBeVisible();
  await page.getByRole("button", { name: "Почнімо!" }).last().click();
  await expect(page.getByRole("button", { name: "Почнімо!" })).toHaveCount(0);
  await page.evaluate(async () => {
    if (document.fullscreenElement) await document.exitFullscreen();
  });
  await page.setViewportSize(landscape);
  await expect(page.getByRole("img", { name: "Поверни пристрій горизонтально" })).toHaveCount(0);
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
