import { expect, test } from "@playwright/test";
import { adminCredentials } from "./support/admin";
import { totp } from "./support/totp";
import { signIn } from "./support/users";

test("admin: login with TOTP → create lesson → generate (stub) → approve → voice → publish", async ({ page }) => {
  const admin = adminCredentials();
  await signIn(page, admin.email, admin.password);
  await expect(page).toHaveURL(/\/admin\/2fa\/verify/);
  await page.getByPlaceholder("000000").fill(totp(admin.totpSecret));
  await page.getByRole("button", { name: "Підтвердити" }).click();
  await expect(page).toHaveURL(/\/admin$/);

  const title = `Урок E2E ${Date.now()}`;
  await page.goto("/admin/lessons");
  await page.getByLabel("Назва").first().fill(title);
  await page.getByRole("button", { name: "Створити урок" }).click();
  const row = page.locator("li, article, div").filter({ hasText: title }).filter({ has: page.getByRole("button", { name: "Згенерувати версію" }) }).last();
  await row.getByRole("button", { name: "Згенерувати версію" }).click();
  const patterns = page.getByRole("checkbox");
  if (!(await patterns.first().isChecked())) await patterns.first().check();
  await page.getByRole("button", { name: "Згенерувати", exact: true }).click();
  await page.getByRole("link", { name: "Перевірити версію →" }).click();

  await expect(page).toHaveURL(/\/moderation/);
  await page.getByRole("button", { name: "Схвалити" }).click();
  await page.getByRole("button", { name: "Озвучити", exact: true }).click();
  await expect(page.getByRole("button", { name: "Опублікувати" })).toBeEnabled();
  await page.getByRole("button", { name: "Опублікувати" }).click();

  await page.goto("/admin/lessons");
  await expect(page.locator("li, article, div").filter({ hasText: title }).getByText("Доступний дітям").first()).toBeVisible();
});
