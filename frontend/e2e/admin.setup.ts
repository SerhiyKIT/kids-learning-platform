import fs from "node:fs";
import { expect, test as setup } from "@playwright/test";
import { api, openLogin } from "./support/api";
import { ADMIN_CREDENTIALS, ADMIN_STATE, STATE_DIR } from "./support/admin";
import { E2E_ENV } from "./support/env";
import { totp } from "./support/totp";
import { PASSWORD, signIn, uniqueEmail } from "./support/users";

/**
 * Scenario 2, first half — the first admin: bootstrap with ADMIN_BOOTSTRAP_TOKEN → login → 2FA
 * setup (TOTP from the shown secret) → backup codes. Runs once per run (bootstrap is one-time per
 * database, which the e2e stack recreates); every spec that needs an admin reuses this session.
 */
setup("first admin: bootstrap, 2FA setup, backup codes", async ({ page }) => {
  const email = uniqueEmail("admin");
  await openLogin(page);
  const boot = await api(page, "POST", "/bootstrap/admin", {
    token: E2E_ENV.ADMIN_BOOTSTRAP_TOKEN,
    email,
    password: PASSWORD,
    displayName: "Адмін E2E",
  });
  expect(boot.status, "bootstrap needs a fresh e2e database (npm run e2e:deps)").toBe(201);
  const again = await api(page, "POST", "/bootstrap/admin", {
    token: E2E_ENV.ADMIN_BOOTSTRAP_TOKEN,
    email: uniqueEmail("admin2"),
    password: PASSWORD,
    displayName: "Другий",
  });
  expect(again.status).toBe(410);

  await signIn(page, email);
  await expect(page).toHaveURL(/\/admin\/2fa\/setup/);
  const secret = (await page.locator("code").first().textContent())!.replace(/\s/g, "");
  await expect(page.getByRole("img").first()).toBeVisible(); // the QR code (a data: URL, CSP-allowed)
  await page.getByPlaceholder("000000").fill(totp(secret));
  await page.getByRole("button", { name: "Увімкнути 2FA" }).click();
  await expect(page.getByText("Коди показуються лише один раз")).toBeVisible();
  const backupCodes = await page.getByRole("listitem").allTextContents();
  expect(backupCodes.length).toBeGreaterThanOrEqual(8);
  await page.getByRole("checkbox").check();
  await page.getByRole("button", { name: "Продовжити" }).click();
  await expect(page).toHaveURL(/\/admin$/);

  fs.mkdirSync(STATE_DIR, { recursive: true });
  fs.writeFileSync(ADMIN_CREDENTIALS, JSON.stringify({ email, password: PASSWORD, totpSecret: secret }), { mode: 0o600 });
  await page.context().storageState({ path: ADMIN_STATE });
});
