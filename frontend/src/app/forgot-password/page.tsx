"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { apiFetch, ApiError, ensureCsrfCookie } from "@/lib/api";
import { AppHeader } from "@/components/ui/AppHeader";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { AuthLayout, Card, CardBody, CardFooter, CardHeader, PageHeading } from "@/components/ui/Card";
import { Field, TextInput } from "@/components/ui/Field";
import { useField } from "@/lib/use-field";

/**
 * Asks for a password-reset email. The answer is the same whether or not the address has an
 * account (no account enumeration), exactly like the backend's.
 */
export default function ForgotPasswordPage() {
  const emailField = useField();
  const email = emailField.value;
  const [submitting, setSubmitting] = useState(false);
  const [sent, setSent] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    ensureCsrfCookie();
  }, []);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await apiFetch("/auth/forgot-password", { method: "POST", body: JSON.stringify({ email }) });
      setSent(true);
    } catch (err) {
      setError(
        err instanceof ApiError && err.status === 429
          ? "Занадто багато запитів. Спробуйте пізніше."
          : "Не вдалося надіслати запит. Спробуйте ще раз.",
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <AppHeader />
      <AuthLayout>
        <PageHeading title="Відновлення пароля" description="Надішлемо посилання для нового пароля на вашу пошту." />
        {sent ? (
          <Card>
            <CardBody>
              <p className="text-ink-soft text-[15px] leading-relaxed text-pretty">
                Якщо для {email} є акаунт, лист із посиланням уже в дорозі. Посилання діє обмежений час.
              </p>
            </CardBody>
            <CardFooter>
              <Link href="/login" className="contents">
                <Button type="button">Повернутися до входу</Button>
              </Link>
            </CardFooter>
          </Card>
        ) : (
          <Card as="form" onSubmit={onSubmit}>
            <CardHeader title="Ваша пошта" description="Та, з якою ви реєструвалися." />
            <CardBody>
              {error ? <Alert title="Помилка">{error}</Alert> : null}
              <Field label="Електронна пошта">
                {({ id }) => (
                  <TextInput id={id} type="email" required {...emailField.inputProps} />
                )}
              </Field>
            </CardBody>
            <CardFooter>
              <Button type="submit" disabled={submitting}>
                {submitting ? "Надсилаємо…" : "Надіслати посилання"}
              </Button>
            </CardFooter>
          </Card>
        )}
      </AuthLayout>
    </>
  );
}
