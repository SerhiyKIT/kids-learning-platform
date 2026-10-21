"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { apiFetch, logout } from "@/lib/api";
import { codeErrorText, twoFactorRedirect } from "@/lib/two-factor";
import { AppHeader } from "@/components/ui/AppHeader";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { AuthLayout, Card, CardBody, CardFooter, CardHeader, PageHeading } from "@/components/ui/Card";
import { CodeInput } from "@/components/ui/CodeInput";
import { Field } from "@/components/ui/Field";

type Mode = "totp" | "backup";

/**
 * Second login step for an admin with 2FA enabled (session PENDING_VERIFICATION). Accepts a TOTP
 * code or an unused backup code — both go to the same POST /admin/2fa/verify. Deliberately calls
 * nothing on mount: in this state the backend allows only /admin/2fa/verify (even /auth/me is 403).
 */
export default function TwoFactorVerifyPage() {
  const router = useRouter();
  const [mode, setMode] = useState<Mode>("totp");
  const [code, setCode] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function switchMode() {
    setMode((m) => (m === "totp" ? "backup" : "totp"));
    setCode("");
    setError(null);
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await apiFetch("/admin/2fa/verify", { method: "POST", body: JSON.stringify({ code: code.replace(/\s/g, "") }) });
      router.replace("/admin");
    } catch (err) {
      const twoFactorTarget = twoFactorRedirect(err);
      if (twoFactorTarget) {
        router.replace(twoFactorTarget);
        return;
      }
      setError(codeErrorText(err));
      setCode("");
    } finally {
      setSubmitting(false);
    }
  }

  async function onLogout() {
    await logout();
    router.replace("/login");
  }

  const isTotp = mode === "totp";
  const canSubmit = isTotp ? code.length === 6 : code.trim().length > 0;

  return (
    <>
      <AppHeader context="Адміністратор" onLogout={onLogout} />
      <AuthLayout>
        <PageHeading
          title="Підтвердження входу"
          description="Акаунт адміністратора захищено двофакторною автентифікацією."
        />
        <Card as="form" onSubmit={onSubmit}>
          <CardHeader
            title={isTotp ? "Код із застосунку" : "Резервний код"}
            description={
              isTotp
                ? "Введіть шість цифр із застосунку-автентифікатора."
                : "Введіть один із резервних кодів, збережених під час налаштування. Кожен діє лише раз."
            }
          />
          <CardBody>
            {error ? <Alert title="Не вдалося підтвердити">{error}</Alert> : null}
            <Field label={isTotp ? "Код підтвердження" : "Резервний код"}>
              {({ id }) =>
                isTotp ? (
                  <CodeInput
                    key="totp"
                    id={id}
                    required
                    autoFocus
                    inputMode="numeric"
                    autoComplete="one-time-code"
                    pattern="[0-9]{6}"
                    maxLength={6}
                    placeholder="000000"
                    invalid={error !== null}
                    value={code}
                    onChange={(e) => setCode(e.target.value.replace(/\D/g, ""))}
                  />
                ) : (
                  <CodeInput
                    key="backup"
                    id={id}
                    required
                    autoFocus
                    autoComplete="off"
                    maxLength={32}
                    placeholder="Резервний код"
                    invalid={error !== null}
                    value={code}
                    onChange={(e) => setCode(e.target.value)}
                    className="text-[22px] tracking-[0.12em]"
                  />
                )
              }
            </Field>
            <button
              type="button"
              onClick={switchMode}
              className="text-brand self-start cursor-pointer text-[15px] underline underline-offset-[3px]"
            >
              {isTotp ? "Використати резервний код" : "Використати код із застосунку"}
            </button>
          </CardBody>
          <CardFooter>
            <Button type="submit" disabled={submitting || !canSubmit}>
              {submitting ? "Перевіряємо…" : "Підтвердити"}
            </Button>
          </CardFooter>
        </Card>
      </AuthLayout>
    </>
  );
}
