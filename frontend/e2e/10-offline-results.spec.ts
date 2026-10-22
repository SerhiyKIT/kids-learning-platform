import { expect, test } from "@playwright/test";
import { api } from "./support/api";
import { teacherGroupWithLesson } from "./support/content";
import { playStubLesson } from "./support/kid-play";
import { parentWithChild } from "./support/users";

// QA1-026: the network drops mid-lesson. The lesson goes on (UX guide §8), and the child's answers
// and the completion used to be dropped on the floor; they now wait and are sent once back online.
test("offline mid-lesson: the child finishes, and the results reach the server once the network is back", async ({ browser }) => {
  const group = await teacherGroupWithLesson(browser);
  const parent = await parentWithChild(browser, "Тестова Дитина");
  const page = parent.page;
  expect((await api(page, "POST", "/groups/join", { joinCode: group.joinCode, childId: parent.childId })).status).toBe(200);
  await api(page, "PUT", `/children/${parent.childId}/play-settings`, { sessionLimitMinutes: null });

  await page.goto(`/play/${parent.childId}`);
  await page.getByRole("button", { name: "Почнімо!" }).first().click();
  await page.locator(".kid-shelf a").first().click();
  await page.locator('button[aria-label="Далі"]').waitFor();

  await parent.context.setOffline(true);
  await playStubLesson(page, false);
  await expect(page.locator('[data-phase="celebrate"]')).toBeVisible();
  await parent.context.setOffline(false);

  await expect
    .poll(async () => {
      const history = await api<{ result: string | null; answers: unknown[] }[]>(page, "GET", `/children/${parent.childId}/history`);
      return history.json.map((h) => [h.result, h.answers.length > 0]);
    }, { timeout: 45_000 })
    .toEqual([["completed", true]]);
  const stickers = await api<{ stickers: unknown[] }>(page, "GET", `/children/${parent.childId}/stickers`);
  expect(stickers.json.stickers).toHaveLength(1);
  await group.close();
  await parent.context.close();
});
