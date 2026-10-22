import fs from "node:fs";
import { expect, test } from "@playwright/test";
import { api } from "./support/api";
import { teacherGroupWithLesson } from "./support/content";
import { playStubLesson } from "./support/kid-play";
import { PASSWORD, parentWithChild, signIn } from "./support/users";

test("privacy: an attempt played offline reaches the child export → sign out everywhere from another device → delete account → can't log in", async ({ browser }) => {
  const group = await teacherGroupWithLesson(browser);
  const parent = await parentWithChild(browser, "Тарас");
  const page = parent.page;
  expect((await api(page, "POST", "/groups/join", { joinCode: group.joinCode, childId: parent.childId })).status).toBe(200);

  // QA1-026: the child plays with the network gone mid-lesson. The lesson goes on, and the answers
  // and the completion reach the server once the network is back — the export below proves it.
  await page.goto(`/play/${parent.childId}`);
  await page.getByRole("button", { name: "Почнімо!" }).first().click();
  await page.locator(".kid-shelf a").first().click();
  await page.locator('button[aria-label="Далі"]').waitFor();
  await parent.context.setOffline(true);
  await playStubLesson(page, false);
  await expect(page.locator('[data-phase="celebrate"]')).toBeVisible();
  await parent.context.setOffline(false);
  await expect
    .poll(async () => (await api<{ result: string | null }[]>(page, "GET", `/children/${parent.childId}/history`)).json.map((h) => h.result), {
      timeout: 45_000,
    })
    .toEqual(["completed"]);

  await page.goto(`/children/${parent.childId}`);
  const [download] = await Promise.all([
    page.waitForEvent("download"),
    page.getByRole("button", { name: "Завантажити дані дитини" }).click(),
  ]);
  const exported = JSON.parse(fs.readFileSync((await download.path())!, "utf8"));
  expect(exported.child.id).toBe(parent.childId);
  expect(exported.attempts).toHaveLength(1);
  expect(exported.attempts[0].answers[0].sceneKey).toBe("scene_2");

  // A second device.
  const other = await browser.newContext();
  const otherPage = await other.newPage();
  await signIn(otherPage, parent.email);
  expect((await api(otherPage, "GET", "/auth/me")).status).toBe(200);

  await page.goto("/account");
  await page.getByRole("button", { name: "Вийти на всіх пристроях" }).click();
  await page.getByRole("button", { name: "Вийти скрізь" }).click();
  await expect(page).toHaveURL(/\/login/);
  expect((await api(otherPage, "GET", "/auth/me")).status).toBe(401);
  await other.close();

  await signIn(page, parent.email);
  await page.goto("/account");
  await expect(page.locator('[data-fate="deleted"]')).toContainText("Тарас");
  await page.getByRole("button", { name: "Видалити акаунт…" }).click();
  const dialog = page.getByRole("dialog");
  await dialog.getByLabel("Ваш пароль").fill(PASSWORD);
  await dialog.getByLabel(/Введіть слово/).fill("ВИДАЛИТИ");
  await dialog.getByRole("button", { name: "Видалити назавжди" }).click();
  await expect(page.getByRole("heading", { name: "Акаунт видалено" })).toBeVisible();

  await page.goto("/login");
  await page.getByLabel("Електронна пошта").fill(parent.email);
  await page.locator("#password").fill(PASSWORD);
  await page.getByRole("button", { name: "Увійти" }).click();
  await expect(page.getByText(/Невірний email або пароль/)).toBeVisible();

  await group.close();
  await parent.context.close();
});
