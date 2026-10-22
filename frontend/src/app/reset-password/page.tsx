"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import { apiFetch, ensureCsrfCookie } from "@/lib/api";
import { PASSWORD_TOO_LONG_TEXT, passwordTooLong } from "@/lib/password";
import { AppHeader } from "@/components/ui/AppHeader";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { AuthLayout, Card, CardBody, CardFooter, CardHeader, PageHeading } from "@/components/ui/Card";
import { Field, PasswordInput } from "@/components/ui/Field";
import { useField } from "@/lib/use-field";

/** Same minimum as registration (backend ResetPasswordRequest, @Size(min = 10)). */
const MIN_PASSWORD_LENGTH = 10;

export default function ResetPasswordPage() {
  // useSearchParams() needs a Suspense boundary to keep the rest of the route prerenderable.
  return (
    <Suspense fallback={<AppHeader />}>
      <ResetPasswordScreen />
    </Suspense>
  );
}

/**
 * Landing page of the password-reset email. The token goes only to POST /api/auth/reset-password
 * and is dropped from the address bar once used; afterwards every session of the account ends.
 */
function ResetPasswordScreen() {
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

  const tooLong = passwordTooLong(password);
  const canSubmit = token !== "" && password.length >= MIN_PASSWORD_LENGTH && !tooLong && password === confirm;

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!canSubmit) return;
    setSubmitting(true);
    setError(null);
    try {
      await apiFetch("/auth/reset-password", { method: "POST", body: JSON.stringify({ token, newPassword: password }) });
      setDone(true);
      passwordField.set("");
      confirmField.set("");
      router.replace("/reset-password");
    } catch {
      setError("Посилання застаріло або вже використане. Запросіть нове на сторінці «Забули пароль?».");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <AppHeader />
      <AuthLayout>
        <PageHeading title="Новий пароль" description="Після зміни пароля всі пристрої вийдуть з акаунта." />
        {done ? (
          <Card>
            <CardBody>
              <p className="text-ink-soft text-[15px]">Пароль змінено. Увійдіть з новим паролем.</p>
            </CardBody>
            <CardFooter>
              <Link href="/login" className="contents">
                <Button type="button">Увійти</Button>
              </Link>
            </CardFooter>
          </Card>
        ) : (
          <Card as="form" onSubmit={onSubmit}>
            <CardHeader title="Придумайте пароль" description={`Щонайменше ${MIN_PASSWORD_LENGTH} символів.`} />
            <CardBody>
              {error || token === "" ? <Alert title="Не вдалося">{error ?? "Посилання недійсне."}</Alert> : null}
              <Field label="Новий пароль" error={tooLong ? PASSWORD_TOO_LONG_TEXT : undefined}>
                {({ id }) => (
                  <PasswordInput id={id} required minLength={MIN_PASSWORD_LENGTH} invalid={tooLong} autoComplete="new-password" {...passwordField.inputProps} />
                )}
              </Field>
              <Field label="Повторіть пароль">
                {({ id }) => (
                  <PasswordInput id={id} required minLength={MIN_PASSWORD_LENGTH} autoComplete="new-password" {...confirmField.inputProps} />
                )}
              </Field>
            </CardBody>
            <CardFooter>
              <Button type="submit" disabled={submitting || !canSubmit}>
                {submitting ? "Зберігаємо…" : "Зберегти пароль"}
              </Button>
            </CardFooter>
          </Card>
        )}
      </AuthLayout>
    </>
  );
}
