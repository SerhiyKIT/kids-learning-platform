"use client";

import Image from "next/image";
import { useRouter } from "next/navigation";
import QRCode from "qrcode";
import { useEffect, useRef, useState } from "react";
import { apiFetch, ApiError, logout } from "@/lib/api";
import {
  codeErrorText,
  twoFactorRedirect,
  type TwoFactorEnableResponse,
  type TwoFactorSetupResponse,
} from "@/lib/two-factor";
import { AppHeader } from "@/components/ui/AppHeader";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { AuthLayout, Card, CardBody, CardFooter, CardHeader, PageHeading } from "@/components/ui/Card";
import { CodeInput } from "@/components/ui/CodeInput";
import { CopyButton } from "@/components/ui/CopyButton";
import { Field } from "@/components/ui/Field";
import { Notice } from "@/components/ui/Notice";
import { QuietButton } from "@/components/ui/SecondaryButton";

/**
 * First-time admin 2FA setup: POST /admin/2fa/setup → QR + manual secret → POST /admin/2fa/enable
 * → one-time backup codes. The secret, the QR (rendered locally from otpauthUri — never sent to
 * any third-party QR service) and the backup codes live only in this component's state: never in
 * storage, the URL or the console, and gone when the page unmounts.
 */
export default function TwoFactorSetupPage() {
  const router = useRouter();
  const [setup, setSetup] = useState<TwoFactorSetupResponse | null>(null);
  const [qrDataUrl, setQrDataUrl] = useState<string | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [backupCodes, setBackupCodes] = useState<string[] | null>(null);

  // One setup request per mount: each call replaces the pending secret server-side, so React's
  // dev-only double effect run must reuse the same in-flight request instead of firing a second.
  const setupRequest = useRef<Promise<TwoFactorSetupResponse> | null>(null);

  useEffect(() => {
    let active = true;
    setupRequest.current ??= apiFetch<TwoFactorSetupResponse>("/admin/2fa/setup", { method: "POST" });
    setupRequest.current
      .then((data) =>
        QRCode.toDataURL(data.otpauthUri, { margin: 1, width: 224 }).then((url) => {
          if (!active) return;
          setSetup(data);
          setQrDataUrl(url);
        }),
      )
      .catch((err) => {
        if (!active) return;
        const twoFactorTarget = twoFactorRedirect(err);
        if (twoFactorTarget) {
          router.replace(twoFactorTarget);
          return;
        }
        if (err instanceof ApiError && err.status === 401) {
          router.replace("/login");
          return;
        }
        // 409 TWO_FACTOR_ALREADY_ENABLED: this admin is past setup already.
        if (err instanceof ApiError && err.status === 409) {
          router.replace("/admin");
          return;
        }
        setLoadError(
          err instanceof ApiError && err.status === 403
            ? "Ця сторінка лише для адміністраторів."
            : "Не вдалося почати налаштування. Оновіть сторінку.",
        );
      });
    return () => {
      active = false;
    };
  }, [router]);

  async function onLogout() {
    await logout();
    router.replace("/login");
  }

  return (
    <>
      <AppHeader context="Адміністратор" onLogout={onLogout} />
      <AuthLayout>
        {backupCodes ? (
          <BackupCodesStep codes={backupCodes} onDone={() => router.replace("/admin")} />
        ) : (
          <>
            <PageHeading
              title="Двофакторна автентифікація"
              description="Для адміністраторів вона обов'язкова. Налаштуйте застосунок-автентифікатор, щоб продовжити."
            />
            {loadError ? <Alert title="Помилка">{loadError}</Alert> : null}
            {!setup && !loadError ? <p className="text-ink-muted">Готуємо ключ…</p> : null}
            {setup && qrDataUrl ? (
              <EnableStep setup={setup} qrDataUrl={qrDataUrl} onEnabled={setBackupCodes} />
            ) : null}
          </>
        )}
      </AuthLayout>
    </>
  );
}

function EnableStep({
  setup,
  qrDataUrl,
  onEnabled,
}: {
  setup: TwoFactorSetupResponse;
  qrDataUrl: string;
  onEnabled: (codes: string[]) => void;
}) {
  const router = useRouter();
  const [code, setCode] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const result = await apiFetch<TwoFactorEnableResponse>("/admin/2fa/enable", {
        method: "POST",
        body: JSON.stringify({ code }),
      });
      onEnabled(result.backupCodes);
    } catch (err) {
      const twoFactorTarget = twoFactorRedirect(err);
      if (twoFactorTarget) {
        router.replace(twoFactorTarget);
        return;
      }
      setError(codeErrorText(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card as="form" onSubmit={onSubmit}>
      <CardHeader
        title="1. Скануйте QR-код"
        description="Відкрийте Google Authenticator, 1Password, Authy або інший TOTP-застосунок і додайте новий акаунт."
      />
      <CardBody>
        <div className="flex justify-center">
          {/* A data: URL generated in this browser: nothing for the optimizer to fetch. */}
          <Image
            unoptimized
            src={qrDataUrl}
            alt="QR-код для застосунку-автентифікатора"
            width={224}
            height={224}
            className="border-line rounded-lg border"
          />
        </div>
        <div className="flex flex-col gap-2">
          <p className="text-ink-soft text-sm">Не можете сканувати? Введіть ключ вручну:</p>
          <div className="flex flex-wrap items-center gap-3">
            <code className="bg-canvas border-line flex-1 rounded-lg border px-3.5 py-2.5 font-mono text-[15px] tracking-[0.06em] break-all">
              {groupsOfFour(setup.secretBase32)}
            </code>
            <CopyButton text={setup.secretBase32} />
          </div>
        </div>
      </CardBody>
      <CardHeader title="2. Введіть код із застосунку" description="Шість цифр, що оновлюються кожні 30 секунд." />
      <CardBody>
        {error ? <Alert title="Не вдалося підтвердити">{error}</Alert> : null}
        <Field label="Код підтвердження">
          {({ id }) => (
            <CodeInput
              id={id}
              required
              inputMode="numeric"
              autoComplete="one-time-code"
              pattern="[0-9]{6}"
              maxLength={6}
              placeholder="000000"
              invalid={error !== null}
              value={code}
              onChange={(e) => setCode(e.target.value.replace(/\D/g, ""))}
            />
          )}
        </Field>
      </CardBody>
      <CardFooter>
        <Button type="submit" disabled={submitting || code.length !== 6}>
          {submitting ? "Перевіряємо…" : "Увімкнути 2FA"}
        </Button>
      </CardFooter>
    </Card>
  );
}

function BackupCodesStep({ codes, onDone }: { codes: string[]; onDone: () => void }) {
  const [saved, setSaved] = useState(false);
  const allCodes = codes.join("\n");

  function onDownload() {
    const blob = new Blob([`Резервні коди KidLearn Admin\n\n${allCodes}\n`], { type: "text/plain;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "kidlearn-backup-codes.txt";
    link.click();
    URL.revokeObjectURL(url);
  }

  return (
    <>
      <PageHeading
        title="Резервні коди"
        description="2FA увімкнено. Кожен код можна використати один раз, якщо немає доступу до застосунку."
      />
      <Notice title="Коди показуються лише один раз">
        Збережіть їх зараз у надійному місці. Після переходу далі побачити їх знову буде неможливо.
      </Notice>
      <Card>
        <CardBody>
          <ul className="grid grid-cols-2 gap-x-6 gap-y-2.5 font-mono text-[17px] tracking-[0.08em]">
            {codes.map((code) => (
              <li key={code}>{code}</li>
            ))}
          </ul>
          <div className="flex flex-wrap gap-2.5">
            <CopyButton text={allCodes} label="Копіювати все" />
            <QuietButton type="button" onClick={onDownload}>
              Завантажити .txt
            </QuietButton>
          </div>
        </CardBody>
        <CardFooter>
          <label className="flex cursor-pointer items-center gap-2.5 text-[15px]">
            <input
              type="checkbox"
              checked={saved}
              onChange={(e) => setSaved(e.target.checked)}
              className="size-4.5 accent-[color:var(--color-brand)]"
            />
            Я зберіг коди
          </label>
          <Button type="button" disabled={!saved} onClick={onDone}>
            Продовжити
          </Button>
        </CardFooter>
      </Card>
    </>
  );
}

function groupsOfFour(secret: string): string {
  return secret.match(/.{1,4}/g)?.join(" ") ?? secret;
}
