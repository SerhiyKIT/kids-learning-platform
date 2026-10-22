/**
 * The backend hashes passwords with BCrypt, which reads at most 72 bytes (backend FitsBcrypt).
 * A Cyrillic letter takes two bytes in UTF-8, so the limit is 36 Cyrillic or 72 Latin characters.
 */
const MAX_PASSWORD_BYTES = 72;

export const PASSWORD_TOO_LONG_TEXT = "Задовгий пароль: до 72 латинських символів або до 36 літер кирилицею.";

export function passwordTooLong(password: string): boolean {
  return new TextEncoder().encode(password).length > MAX_PASSWORD_BYTES;
}
