import fs from "node:fs";
import path from "node:path";
import type { Browser } from "@playwright/test";

/** Written by admin.setup.ts: the one bootstrapped admin and its verified session. */
export const STATE_DIR = path.join(__dirname, "..", ".state");
export const ADMIN_STATE = path.join(STATE_DIR, "admin-session.json");
export const ADMIN_CREDENTIALS = path.join(STATE_DIR, "admin-credentials.json");

export interface AdminCredentials {
  email: string;
  password: string;
  totpSecret: string;
}

export function adminCredentials(): AdminCredentials {
  return JSON.parse(fs.readFileSync(ADMIN_CREDENTIALS, "utf8")) as AdminCredentials;
}

/** A page signed in as the admin, 2FA already verified (reuses the setup's session). */
export async function adminPage(browser: Browser) {
  const context = await browser.newContext({ storageState: ADMIN_STATE });
  const page = await context.newPage();
  await page.goto("/admin");
  return { context, page };
}
