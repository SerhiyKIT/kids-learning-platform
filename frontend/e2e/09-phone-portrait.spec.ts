import { expect, test } from "@playwright/test";
import { parentWithChild } from "./support/users";

// QA1-023: a phone held (or rotation-locked) in portrait used to see only the "turn the device"
// picture, with «Почнімо!» — the tap that asks for fullscreen + a landscape lock — hidden under it.
test("child mode in portrait: «Почнімо!» is reachable, the adult gets a hint, turning shows the shelf @phone", async ({ browser }) => {
  const parent = await parentWithChild(browser, "Тестова Дитина");
  const page = parent.page;
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto(`/play/${parent.childId}`);

  await expect(page.getByRole("img", { name: "Поверни пристрій горизонтально" })).toBeVisible();
  await expect(page.getByText("Для дорослого: якщо екран не повертається")).toBeVisible();
  await page.getByRole("button", { name: "Почнімо!" }).last().click();

  // Started, still in portrait: the picture stays, now without the start button.
  await expect(page.getByRole("img", { name: "Поверни пристрій горизонтально" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Почнімо!" })).toHaveCount(0);

  await page.evaluate(async () => {
    if (document.fullscreenElement) await document.exitFullscreen();
  });
  await page.setViewportSize({ width: 844, height: 390 });
  await expect(page.getByRole("img", { name: "Поверни пристрій горизонтально" })).toHaveCount(0);
  await expect(page.getByRole("link", { name: "Мої наліпки" })).toBeVisible();
  await parent.context.close();
});
