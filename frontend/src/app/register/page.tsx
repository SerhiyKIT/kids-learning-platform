"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { apiFetch, ApiError, ensureCsrfCookie } from "@/lib/api";
import type { DevRegisterRoleRequest, DevRegisterRoleResponse, Role } from "@/lib/api-types";
import { AppHeader } from "@/components/ui/AppHeader";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { AuthLayout, Card, CardBody, CardFooter, CardHeader, PageHeading } from "@/components/ui/Card";
import { Field, PasswordInput, Select, TextInput } from "@/components/ui/Field";

interface RegisterResponse {
  id: string;
  email: string;
  displayName: string;
}

const IS_DEV = process.env.NODE_ENV !== "production";

const DEV_ROLE_LABELS: Record<Role, string> = {
  PARENT: "Батьки",
  TEACHER: "Вчитель",
  ADMIN: "Адміністратор",
};

export default function RegisterPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [devRole, setDevRole] = useState<Role>("PARENT");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<ApiError | null>(null);
  const [registered, setRegistered] = useState(false);

  useEffect(() => {
    ensureCsrfCookie();
  }, []);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      if (IS_DEV && devRole !== "PARENT") {
        // Dev-only shortcut: the only way to obtain a TEACHER/ADMIN login, since the normal
        // endpoint below always creates a PARENT. The account comes back already verified, so
        // there's no "check your email" step — straight to login.
        const body: DevRegisterRoleRequest = { email, password, displayName, role: devRole };
        await apiFetch<DevRegisterRoleResponse>("/dev/register-role", {
          method: "POST",
          body: JSON.stringify(body),
        });
        router.push("/login");
        return;
      }
      await apiFetch<RegisterResponse>("/auth/register", {
        method: "POST",
        body: JSON.stringify({ email, password, displayName }),
      });
      setRegistered(true);
    } catch (err) {
      setError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка"));
    } finally {
      setSubmitting(false);
    }
  }

  if (registered) {
    return (
      <>
        <AppHeader />
        <AuthLayout>
          <PageHeading
            title="Перевірте пошту"
            description={`Ми надіслали лист із посиланням на ${email}. Підтвердіть адресу, щоб увійти до кабінету.`}
          />
          <Card>
            <CardBody>
              <p className="text-ink-soft text-[15px] leading-relaxed text-pretty">
                Лист не прийшов протягом кількох хвилин? Перевірте теку «Спам» — або повторіть
                реєстрацію з іншою адресою.
              </p>
            </CardBody>
            <CardFooter>
              <Link href="/login" className="contents">
                <Button type="button">Перейти до входу</Button>
              </Link>
            </CardFooter>
          </Card>
        </AuthLayout>
      </>
    );
  }

  const emailError = fieldError(error, "email");

  return (
    <>
      <AppHeader />
      <AuthLayout>
        <PageHeading
          title="Реєстрація"
          description="Один акаунт для батьків, вчителів та адміністраторів. Дитячі профілі створюються окремо після входу."
        />

        <Card as="form" onSubmit={onSubmit}>
          <CardHeader
            title="Створення акаунту"
            description="Кабінет дорослого: керування профілями дітей, звіти та налаштування безпеки."
          />

          <CardBody>
            {error && !emailError ? (
              <Alert title="Не вдалося зареєструватися">{errorText(error)}</Alert>
            ) : null}

            <Field label="Електронна пошта" error={emailError}>
              {({ id, describedBy }) => (
                <TextInput
                  id={id}
                  aria-describedby={describedBy}
                  type="email"
                  required
                  invalid={Boolean(emailError)}
                  placeholder="name@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                />
              )}
            </Field>

            <Field label="Пароль" hint="Мінімум 10 символів.">
              {({ id, describedBy }) => (
                <PasswordInput
                  id={id}
                  aria-describedby={describedBy}
                  required
                  minLength={10}
                  placeholder="Введіть пароль"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
              )}
            </Field>

            <Field label="Ім'я">
              {({ id }) => (
                <TextInput
                  id={id}
                  type="text"
                  required
                  placeholder="Олена Ковальчук"
                  value={displayName}
                  onChange={(e) => setDisplayName(e.target.value)}
                />
              )}
            </Field>

            {IS_DEV ? (
              <div className="bg-dev-surface border-dev-line flex flex-col gap-2.25 rounded-lg border border-dashed p-4">
                <div className="flex items-center gap-2.25">
                  <span
                    aria-hidden="true"
                    className="bg-dev-mark size-2 flex-none rotate-45"
                  />
                  <label
                    htmlFor="dev-role"
                    className="text-dev-ink font-mono text-[13px] font-medium tracking-[0.09em] uppercase"
                  >
                    Роль (лише для розробки)
                  </label>
                </div>
                <Select
                  id="dev-role"
                  className="border-dev-line-field"
                  value={devRole}
                  onChange={(e) => setDevRole(e.target.value as Role)}
                >
                  {(Object.entries(DEV_ROLE_LABELS) as [Role, string][]).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </Select>
                <p className="text-dev-ink text-[13px] leading-snug">
                  Це поле не відображається у продакшн-збірці.
                </p>
              </div>
            ) : null}
          </CardBody>

          <CardFooter>
            <Button type="submit" disabled={submitting}>
              {submitting ? "Реєструємо…" : "Зареєструватися"}
            </Button>
            <p className="text-ink-muted flex items-center justify-center gap-2 text-[15px]">
              <span>Вже маю акаунт →</span>
              <Link href="/login" className="font-medium underline underline-offset-[3px]">
                Вхід
              </Link>
            </p>
          </CardFooter>
        </Card>

        <p className="text-ink-faint text-[13px] leading-relaxed">
          Реєструючись, ви погоджуєтесь з <Link href="/terms">умовами використання</Link> та{" "}
          <Link href="/privacy">політикою конфіденційності</Link>.
        </p>
      </AuthLayout>
    </>
  );
}

/** 409 і валідаційні problems по email показуємо під самим полем, а не окремим блоком. */
function fieldError(error: ApiError | null, path: string): string | undefined {
  if (!error) return undefined;
  if (error.status === 409 && path === "email") {
    return "Адреса вже зареєстрована або має невірний формат.";
  }
  return error.problems?.find((p) => p.path === path)?.message;
}

function errorText(error: ApiError): string {
  if (error.status === 429) {
    return error.retryAfterSeconds
      ? `Занадто багато спроб. Спробуйте за ${error.retryAfterSeconds} с.`
      : "Занадто багато спроб. Спробуйте пізніше.";
  }
  if (error.problems && error.problems.length > 0) {
    return error.problems.map((p) => p.message).join(" ");
  }
  return error.message;
}
