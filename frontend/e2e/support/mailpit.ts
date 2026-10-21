import { expect } from "@playwright/test";
import { MAILPIT_URL } from "./env";

interface MailpitMessage {
  ID: string;
}

/** Waits for the newest email to `to` and returns the first link in it that contains `path`. */
export async function linkFromEmail(to: string, path: string): Promise<URL> {
  let link: string | undefined;
  await expect
    .poll(
      async () => {
        const search = await fetch(`${MAILPIT_URL}/api/v1/search?query=${encodeURIComponent(`to:"${to}"`)}`);
        const { messages } = (await search.json()) as { messages: MailpitMessage[] };
        for (const message of messages ?? []) {
          const full = (await (await fetch(`${MAILPIT_URL}/api/v1/message/${message.ID}`)).json()) as { Text: string };
          link = full.Text.match(new RegExp(`https?://\\S*${path}\\?token=\\S+`))?.[0];
          if (link) return true;
        }
        return false;
      },
      { timeout: 20_000, message: `email with ${path} to ${to}` },
    )
    .toBe(true);
  return new URL(link!);
}
