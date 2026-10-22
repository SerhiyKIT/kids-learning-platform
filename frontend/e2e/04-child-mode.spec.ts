import { expect, type Request, type Response, test } from "@playwright/test";
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
  // QA1-036: the tap itself starts the lesson, in one round trip — the attempt, the scenario and
  // the album all leave before the attempt's answer is back, and nothing else is waited for.
  const timeline: string[] = [];
  const onRequest = (r: Request) => {
    const path = new URL(r.url()).pathname;
    if (path.startsWith("/api/children/")) timeline.push(`request ${r.method()} ${path.replace(/^\/api\/children\/[^/]+/, "")}`);
  };
  const onResponse = (r: Response) => {
    if (r.request().method() === "POST" && /\/attempts$/.test(r.url())) timeline.push("attempt answered");
  };
  page.on("request", onRequest);
  page.on("response", onResponse);
  await page.locator(".kid-shelf a").first().click();
  await page.locator('button[aria-label="Далі"]').waitFor();
  page.off("request", onRequest);
  page.off("response", onResponse);
  const answered = timeline.indexOf("attempt answered");
  expect(timeline.slice(0, answered)).toEqual(
    expect.arrayContaining(["request POST /attempts", expect.stringMatching(/^request GET \/lessons\/.+\/scenario$/), "request GET /stickers"]),
  );
  expect(timeline.filter((t) => t.includes("available-lessons"))).toEqual([]);
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
  // QA1-031: the teacher sees how each scene went (no answers) — the stub lesson's choice, sorting
  // and control scenes were all done right the first time.
  await expect(group.teacher.locator('[data-scene="scene_2"]').first()).toContainText("Вибір: з 1-ї спроби");
  await expect(group.teacher.locator('[data-scene="scene_3"]').first()).toContainText("Сортування:");
  await expect(group.teacher.locator('[data-scene="scene_4"]').first()).toContainText("Контрольна: з 1-ї спроби");

  // A plain tap on the exit does nothing; holding it opens the adult's password screen.
  await page.getByRole("button", { name: "Вихід для дорослих: утримуйте 2 секунди" }).first().click();
  await expect(page.locator("#kid-exit-password")).toHaveCount(0);
  // QA1-034: a browser that refuses sound even after «Почнімо!» — the adult gets a quiet hint in
  // a corner; the child's screen carries on with no error.
  const silent = await parent.context.newPage();
  await silent.addInitScript(() => {
    HTMLMediaElement.prototype.play = () => Promise.reject(new DOMException("Sound is off", "NotAllowedError"));
  });
  await silent.goto(`/play/${parent.childId}`);
  await expect(silent.getByRole("note")).toHaveCount(0);
  await silent.getByRole("button", { name: "Почнімо!" }).first().click();
  await expect(silent.getByRole("note")).toContainText("Для дорослого: звук не вмикається");
  await expect(silent.locator(".kid-shelf a").first()).toBeVisible();
  await silent.close();

  // QA1-035: the exit dialog keeps keyboard focus inside and Esc cancels it.
  const exitButton = page.getByRole("button", { name: "Вихід для дорослих: утримуйте 2 секунди" }).first();
  const box = (await exitButton.boundingBox())!;
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
  await page.mouse.down();
  await page.waitForTimeout(2300);
  await page.mouse.up();
  await expect(page.locator("#kid-exit-password")).toBeFocused();
  for (const key of ["Tab", "Tab", "Tab", "Tab", "Shift+Tab", "Shift+Tab"]) {
    await page.keyboard.press(key);
    expect(await page.evaluate(() => Boolean(document.activeElement?.closest("[role=dialog]"))), `focus after ${key}`).toBe(true);
  }
  await page.keyboard.press("Escape");
  await expect(page.locator("#kid-exit-password")).toHaveCount(0);
  await exitChildMode(page, PASSWORD);
  await expect(page).toHaveURL(new RegExp(`/children/${parent.childId}$`));

  await group.close();
  await parent.context.close();
});
