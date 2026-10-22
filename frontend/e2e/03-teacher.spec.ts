import { expect, test } from "@playwright/test";
import { adminPage } from "./support/admin";
import { publishedLesson } from "./support/content";
import { linkFromEmail } from "./support/mailpit";
import { PASSWORD, signIn, uniqueEmail } from "./support/users";

test("teacher: invited by the admin → email → set password → login → group → assign a lesson", async ({ browser, page }) => {
  const { page: admin, context: adminContext } = await adminPage(browser);
  const lessonTitle = `Урок для групи ${Date.now()}`;
  await publishedLesson(admin, lessonTitle);

  const email = uniqueEmail("teacher");
  await admin.goto("/admin/teachers");
  await admin.getByRole("button", { name: "Запросити вчителя" }).click();
  await admin.getByLabel("Email").fill(email);
  await admin.getByLabel("Ім'я").fill("Олена Петрівна");
  await admin.getByRole("button", { name: "Надіслати запрошення", exact: true }).click();
  await expect(admin.getByText(email).first()).toBeVisible();
  await adminContext.close();

  const link = await linkFromEmail(email, "/accept-invite");
  await page.goto(link.pathname + link.search);
  // The page sets the CSRF cookie from an effect, i.e. only once hydrated; typing earlier gets reset (QA1-003).
  await page.waitForFunction(() => document.cookie.includes("XSRF-TOKEN="));
  await page.getByLabel("Пароль", { exact: true }).fill(PASSWORD);
  await page.getByLabel("Повторіть пароль").fill(PASSWORD);
  await page.getByRole("button", { name: "Створити пароль" }).click();
  await expect(page.getByRole("link", { name: "Увійти" })).toBeVisible();

  await signIn(page, email);
  await expect(page).toHaveURL(/\/dashboard/);
  await page.goto("/groups");
  await page.getByRole("button", { name: "Створити групу" }).click();
  await page.getByLabel("Назва групи").fill("Сонечко");
  await page.getByRole("button", { name: "Створити", exact: true }).click();
  await page.getByRole("link", { name: /Сонечко/ }).first().click();
  await expect(page).toHaveURL(/\/groups\/.+/);
  await page.getByLabel("Призначити урок").selectOption({ label: `${lessonTitle} (3–4 роки)` });
  await page.getByRole("button", { name: "Призначити", exact: true }).click();
  await expect(page.getByText(lessonTitle).last()).toBeVisible();
});
