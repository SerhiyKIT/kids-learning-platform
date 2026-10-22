import { expect, type Page, test } from "@playwright/test";
import { adminCredentials } from "./support/admin";

// QA1-003: on a slow phone the server-rendered form is on screen seconds before React takes over.
// What the adult types in that time must survive hydration. Hydration is held back here by
// delaying the app's JavaScript; the fields are filled before it arrives.
async function openWithSlowScripts(page: Page, path: string): Promise<void> {
  await page.route("**/_next/static/chunks/**", async (route) => {
    await new Promise((resolve) => setTimeout(resolve, 2_500));
    await route.continue();
  });
  await page.goto(path, { waitUntil: "domcontentloaded" });
}

async function hydrated(page: Page): Promise<void> {
  // The CSRF cookie is requested from an effect, i.e. only once React runs.
  await page.waitForFunction(() => document.cookie.includes("XSRF-TOKEN="), undefined, { timeout: 30_000 });
}

test("login: email and password typed before hydration are kept and sent", async ({ browser }) => {
  const { email, password } = adminCredentials();
  const context = await browser.newContext();
  const page = await context.newPage();
  await openWithSlowScripts(page, "/login");
  await page.getByLabel("Електронна пошта").fill(email);
  await page.locator("#password").fill(password);
  await hydrated(page);
  await expect(page.getByLabel("Електронна пошта")).toHaveValue(email);
  await page.getByRole("button", { name: "Увійти" }).click();
  // The admin's password was right: the next step is the 2FA code.
  await expect(page).toHaveURL(/\/admin\/2fa\/verify/);
  await context.close();
});

for (const [path, button] of [
  ["/accept-invite?token=not-a-real-token", "Створити пароль"],
  ["/reset-password?token=not-a-real-token", "Зберегти пароль"],
] as const) {
  test(`${path.split("?")[0]}: both passwords typed before hydration reach the form's state`, async ({ page }) => {
    await openWithSlowScripts(page, path);
    const fields = page.locator("input[type=password]");
    await fields.nth(0).fill("slow-phone-password-1");
    await fields.nth(1).fill("slow-phone-password-1");
    await hydrated(page);
    await expect(fields.nth(0)).toHaveValue("slow-phone-password-1");
    // Enabled only when React's state holds two matching passwords of 10+ characters.
    await expect(page.getByRole("button", { name: button })).toBeEnabled();
  });
}

test("register and forgot-password keep what was typed before hydration", async ({ page }) => {
  await openWithSlowScripts(page, "/register");
  await page.getByLabel("Електронна пошта").fill("slow@example.test");
  await page.getByLabel("Пароль", { exact: true }).fill("slow-phone-password-1");
  await page.getByLabel("Ім'я").fill("Повільний Телефон");
  await hydrated(page);
  await expect(page.getByLabel("Ім'я")).toHaveValue("Повільний Телефон");
  await expect(page.getByLabel("Пароль", { exact: true })).toHaveValue("slow-phone-password-1");

  await openWithSlowScripts(page, "/forgot-password");
  await page.getByLabel(/пошта/i).fill("slow@example.test");
  await hydrated(page);
  await expect(page.getByLabel(/пошта/i)).toHaveValue("slow@example.test");
});
