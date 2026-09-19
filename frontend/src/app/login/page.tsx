"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { ApiError, ensureCsrfCookie, loginWithFormPost } from "@/lib/api";
import { AppHeader } from "@/components/ui/AppHeader";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { AuthLayout, Card, CardBody, CardFooter, CardHeader, PageHeading } from "@/components/ui/Card";
import { Field, PasswordInput, TextInput } from "@/components/ui/Field";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<ApiError | null>(null);

  useEffect(() => {
    ensureCsrfCookie();
  }, []);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await loginWithFormPost(email, password);
      router.push("/dashboard");
    } catch (err) {
      setError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка"));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <AppHeader />
      <AuthLayout>
        <PageHeading
          title="Вхід"
          description="Увійдіть до кабінету дорослого, щоб переглянути звіти та налаштування безпеки дитини."
        />

        <Card as="form" onSubmit={onSubmit}>
          <CardHeader
            title="Вхід в акаунт"
            description="Використовуйте email, який ви вказали під час реєстрації."
          />

          <CardBody>
            {error ? <Alert title="Не вдалося увійти">{errorText(error)}</Alert> : null}

            <Field label="Електронна пошта">
              {({ id }) => (
                <TextInput
                  id={id}
                  type="email"
                  required
                  placeholder="name@example.com"
                  invalid={error?.status === 401}
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                />
              )}
            </Field>

            <div className="flex flex-col gap-[7px]">
              <div className="flex items-baseline justify-between gap-3">
                <label htmlFor="password" className="text-[15px] font-medium">
                  Пароль
                </label>
                <Link href="/forgot-password" className="text-sm">
                  Забули пароль?
                </Link>
              </div>
              <PasswordInput
                id="password"
                required
                placeholder="Введіть пароль"
                invalid={error?.status === 401}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>
          </CardBody>

          <CardFooter>
            <Button type="submit" disabled={submitting}>
              {submitting ? "Входимо…" : "Увійти"}
            </Button>
            <p className="text-ink-muted flex items-center justify-center gap-2 text-[15px]">
              <span>Ще немає акаунту?</span>
              <Link href="/register" className="font-medium underline underline-offset-[3px]">
                Реєстрація
              </Link>
            </p>
          </CardFooter>
        </Card>

        <p className="text-ink-faint text-[13px] leading-relaxed">
          Дитячі профілі доступні лише після входу дорослого.
        </p>
      </AuthLayout>
    </>
  );
}

function errorText(error: ApiError): string {
  if (error.status === 429) {
    return error.retryAfterSeconds
      ? `Занадто багато спроб. Спробуйте за ${error.retryAfterSeconds} с.`
      : "Занадто багато спроб. Спробуйте пізніше.";
  }
  if (error.status === 401) {
    return "Невірний email або пароль. Перевірте дані та спробуйте ще раз.";
  }
  return error.message;
}
