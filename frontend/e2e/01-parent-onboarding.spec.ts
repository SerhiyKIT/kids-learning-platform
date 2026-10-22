import { expect, test } from "@playwright/test";
import { linkFromEmail, subjectsTo } from "./support/mailpit";
import { PASSWORD, signIn, uniqueEmail } from "./support/users";

test("parent: register → verification email → verify → login in another case → child → consent", async ({ browser, page }) => {
  const email = uniqueEmail("onboarding");
  const typed = `  ${email.replace("e2e-", "E2E-").replace("@example.test", "@Example.TEST")} `;

  await page.goto("/register");
  await page.getByLabel("Електронна пошта").fill(typed);
  await page.getByLabel("Пароль", { exact: true }).fill(PASSWORD);
  await page.getByLabel("Ім'я").fill("Оксана");
  await page.getByRole("button", { name: "Зареєструватися" }).click();
  await expect(page.getByRole("heading", { name: "Перевірте пошту" })).toBeVisible();

  const link = await linkFromEmail(email, "/verify-email");
  await page.goto(link.pathname + link.search);
  await page.getByRole("button", { name: "Підтвердити адресу" }).click();
  await expect(page.getByRole("heading", { name: "Пошту підтверджено" })).toBeVisible();

  await signIn(page, email.toUpperCase());
  await expect(page).toHaveURL(/\/dashboard/);

  await page.getByRole("link", { name: "+ Додати дитину" }).click();
  await page.getByLabel("Ім'я дитини").fill("Марійка");
  await page.getByRole("button", { name: "Додати" }).click();
  await expect(page).toHaveURL(/\/children\//);
  await expect(page.getByText("Очікує згоди")).toBeVisible();
  await page.getByRole("button", { name: "Надати згоду" }).click();
  await expect(page.getByText("Активна")).toBeVisible();
  await expect(page.getByRole("link", { name: "Кабінет дитини → грати" })).toBeVisible();

  // QA1-006: signing up again with the taken address looks exactly like a new sign-up; only the
  // mailbox's owner is told that the account already exists.
  const again = await browser.newPage();
  await again.goto("/register");
  await again.getByLabel("Електронна пошта").fill(email);
  await again.getByLabel("Пароль", { exact: true }).fill("another-password-1");
  await again.getByLabel("Ім'я").fill("Хтось");
  await again.getByRole("button", { name: "Зареєструватися" }).click();
  await expect(again.getByRole("heading", { name: "Перевірте пошту" })).toBeVisible();
  await expect.poll(() => subjectsTo(email), { timeout: 20_000 }).toContain("Ви вже зареєстровані в «Безпечному Просторі»");
  await again.close();
});
