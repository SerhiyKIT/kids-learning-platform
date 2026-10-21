import { expect, type Page } from "@playwright/test";

/** Holds the adult exit button for 2.3 s (the ring fills in 2 s), then signs out of child mode. */
export async function exitChildMode(page: Page, password: string): Promise<void> {
  const exit = page.getByRole("button", { name: "Вихід для дорослих: утримуйте 2 секунди" }).first();
  const box = (await exit.boundingBox())!;
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
  await page.mouse.down();
  await page.waitForTimeout(2300);
  await page.mouse.up();
  await page.locator("#kid-exit-password").fill(password);
  await page.getByRole("button", { name: "Вийти", exact: true }).click();
}

async function tapOrClick(page: Page, selector: string, touch: boolean): Promise<void> {
  const target = page.locator(selector).first();
  if (touch) await target.tap();
  else await target.click();
}

async function dragTo(page: Page, from: string, to: string): Promise<void> {
  const a = (await page.locator(from).first().boundingBox())!;
  const b = (await page.locator(to).first().boundingBox())!;
  await page.mouse.move(a.x + a.width / 2, a.y + a.height / 2);
  await page.mouse.down();
  await page.mouse.move(b.x + b.width / 2, b.y + b.height / 2, { steps: 12 });
  await page.mouse.up();
}

/**
 * Plays the stub lesson to the end: demo → choice (right answer) → sorting (two cards dragged, one
 * placed tap-tap) → control choice. `beforeLastAnswer` runs right before the final answer.
 */
export async function playStubLesson(page: Page, touch: boolean, beforeLastAnswer?: () => Promise<void>): Promise<void> {
  const next = 'button[aria-label="Далі"]';
  await tapOrClick(page, next, touch); // demo narration
  await tapOrClick(page, 'button[aria-label="Look both ways"]', touch);
  await tapOrClick(page, next, touch);

  await page.locator("[data-item-index]").first().waitFor();
  await dragTo(page, '[data-item-index="0"]', '[data-category="cat_1"]');
  await expect(page.locator('[data-item-index="0"]')).toHaveAttribute("aria-disabled", "true");
  await tapOrClick(page, '[data-item-index="1"]', touch);
  await expect(page.locator('[data-item-index="1"]')).toHaveAttribute("aria-pressed", "true");
  await tapOrClick(page, '[data-category="cat_2"]', touch);
  await expect(page.locator('[data-item-index="1"]')).toHaveAttribute("aria-disabled", "true");
  await dragTo(page, '[data-item-index="2"]', '[data-category="cat_1"]');
  await tapOrClick(page, next, touch);

  await page.getByRole("button", { name: "Look both ways" }).first().waitFor();
  if (beforeLastAnswer) await beforeLastAnswer();
  await tapOrClick(page, 'button[aria-label="Look both ways"]', touch);
  await tapOrClick(page, next, touch);
}
