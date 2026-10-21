import { expect, type Page } from "@playwright/test";

/**
 * Child-mode targets (docs/UX_гайд_дитячого_режиму.md §2): every visible interactive element is at
 * least 64×64 CSS px and has an accessible name. The one documented exception is the adult's exit
 * button — deliberately small and in the corner (guide §5 «маленька "доросла" кнопка в куті»).
 */
export async function expectKidTargets(page: Page): Promise<void> {
  const problems = await page.evaluate(() => {
    const exitLabel = "Вихід для дорослих: утримуйте 2 секунди";
    const found: string[] = [];
    for (const el of document.querySelectorAll<HTMLElement>("button, a[href], [role=button], input, select, textarea")) {
      const rect = el.getBoundingClientRect();
      const style = getComputedStyle(el);
      if (rect.width === 0 || rect.height === 0 || style.visibility === "hidden" || el.closest("[aria-hidden=true]")) continue;
      const name = el.getAttribute("aria-label") ?? el.textContent?.trim() ?? "";
      if (name === exitLabel) continue;
      if (!el.getAttribute("aria-label")) found.push(`no aria-label: <${el.tagName.toLowerCase()}> "${name.slice(0, 30)}"`);
      if (rect.width < 63.5 || rect.height < 63.5) found.push(`too small (${Math.round(rect.width)}×${Math.round(rect.height)}): ${name.slice(0, 40)}`);
    }
    return found;
  });
  expect(problems, problems.join("\n")).toEqual([]);
}
