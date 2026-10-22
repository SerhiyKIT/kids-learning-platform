"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import { apiFetch, ApiError, ensureCsrfCookie } from "@/lib/api";
import { PASSWORD_TOO_LONG_TEXT, passwordTooLong } from "@/lib/password";
import { AppHeader } from "@/components/ui/AppHeader";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { AuthLayout, Card, CardBody, CardFooter, CardHeader, PageHeading } from "@/components/ui/Card";
import { Field, PasswordInput } from "@/components/ui/Field";
import { useField } from "@/lib/use-field";

/** Same minimum as registration / password reset (backend AcceptInviteRequest, @Size(min = 10)). */
const MIN_PASSWORD_LENGTH = 10;

export default function AcceptInvitePage() {
  // useSearchParams() needs a Suspense boundary to keep the rest of the route prerenderable.
  return (
    <Suspense fallback={<AppHeader />}>
      <AcceptInviteScreen />
    </Suspense>
  );
}

/**
 * Public landing page for a teacher's invitation link. The token is read from the URL once into
 * component state, sent only to POST /api/auth/accept-invite, never logged or stored, and dropped
 * from the address bar as soon as it has been used. Doesn't log the teacher in: they sign in next.
 */
function AcceptInviteScreen() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [token] = useState(() => searchParams.get("token") ?? "");
  const passwordField = useField();
  const password = passwordField.value;
  const confirmField = useField();
  const confirm = confirmField.value;
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);

  useEffect(() => {
    ensureCsrfCookie();
  }, []);

  const tooShort = password.length > 0 && password.length < MIN_PASSWORD_LENGTH;
  const tooLong = passwordTooLong(password);
  const mismatch = confirm.length > 0 && confirm !== password;
  const canSubmit = token !== "" && password.length >= MIN_PASSWORD_LENGTH && !tooLong && password === confirm;

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!canSubmit || submitting) return;
    setSubmitting(true);
    setError(null);
    try {
      await apiFetch("/auth/accept-invite", { method: "POST", body: JSON.stringify({ token, password }) });
      setDone(true);
      passwordField.set("");
      confirmField.set("");
      // The token is spent — keep it out of the address bar and history.
      router.replace("/accept-invite");
    } catch (err) {
      if (err instanceof ApiError && err.status === 429) {
        setError(
          err.retryAfterSeconds
            ? `Занадто багато спроб. Спробуйте за ${err.retryAfterSeconds} с.`
            : "Занадто багато спроб. Спробуйте пізніше.",
        );
      } else if (err instanceof ApiError && err.status === 400) {
        setError("Посилання недійсне або прострочене. Попросіть адміністратора надіслати нове.");
      } else {
        setError("Несподівана помилка. Спробуйте ще раз.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  if (done) {
    return (
      <>
        <AppHeader />
        <AuthLayout>
          <PageHeading title="Пароль створено" description="Тепер увійдіть до кабінету вчителя з цим паролем." />
          <Link href="/login" className="no-underline">
            <Button type="button">Увійти</Button>
          </Link>
        </AuthLayout>
      </>
    );
  }

  return (
    <>
      <AppHeader />
      <AuthLayout>
        <PageHeading
          title="Вітаємо!"
          description="Вас запросили до платформи як вчителя. Створіть пароль, щоб увійти."
        />
        <Card as="form" onSubmit={onSubmit}>
          <CardHeader title="Створіть пароль" description={`Щонайменше ${MIN_PASSWORD_LENGTH} символів.`} />
          <CardBody>
            {!token ? (
              <Alert title="Посилання неповне">
                Відкрийте посилання з листа повністю або попросіть адміністратора надіслати нове.
              </Alert>
            ) : null}
            {error ? <Alert title="Не вдалося створити пароль">{error}</Alert> : null}
            <Field label="Пароль" error={tooShort ? `Щонайменше ${MIN_PASSWORD_LENGTH} символів` : tooLong ? PASSWORD_TOO_LONG_TEXT : undefined}>
              {({ id }) => (
                <PasswordInput
                  id={id}
                  required
                  autoComplete="new-password"
                  invalid={tooShort || tooLong}
                  {...passwordField.inputProps}
                />
              )}
            </Field>
            <Field label="Повторіть пароль" error={mismatch ? "Паролі не збігаються" : undefined}>
              {({ id }) => (
                <PasswordInput
                  id={id}
                  required
                  autoComplete="new-password"
                  invalid={mismatch}
                  {...confirmField.inputProps}
                />
              )}
            </Field>
          </CardBody>
          <CardFooter>
            <Button type="submit" disabled={!canSubmit || submitting}>
              {submitting ? "Зберігаємо…" : "Створити пароль"}
            </Button>
          </CardFooter>
        </Card>
      </AuthLayout>
    </>
  );
}
