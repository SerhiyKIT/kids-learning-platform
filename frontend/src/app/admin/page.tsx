"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { apiFetch, ApiError, logout } from "@/lib/api";
import type { PendingReviewEntry, VoicingResult } from "@/lib/admin-types";
import type { Me } from "@/lib/api-types";
import { twoFactorRedirect } from "@/lib/two-factor";
import { AppHeader } from "@/components/ui/AppHeader";
import { Pill } from "@/components/ui/Pill";
import { SecondaryButton } from "@/components/ui/SecondaryButton";

/**
 * Admin home. Order matters: /auth/me first (non-admins go back to /dashboard; a session pending
 * verification already gets a 2FA 403 here), then /admin/ping — the first truly gated admin call,
 * so a setup-required session is sent to /admin/2fa/setup before any admin UI renders.
 */
export default function AdminHomePage() {
  const router = useRouter();
  const [me, setMe] = useState<Me | null>(null);
  const [queueCount, setQueueCount] = useState<number | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    apiFetch<Me>("/auth/me")
      .then((meData) => {
        if (meData.role !== "ADMIN") {
          router.replace("/dashboard");
          return;
        }
        return apiFetch<string>("/admin/ping").then(() => {
          setMe(meData);
          return apiFetch<PendingReviewEntry[]>("/admin/lesson-versions?status=auto_validated")
            .then((queue) => setQueueCount(queue.length))
            // The count is a nicety on the card; the page works without it.
            .catch(() => setQueueCount(null));
        });
      })
      .catch((err) => {
        const twoFactorTarget = twoFactorRedirect(err);
        if (twoFactorTarget) {
          router.replace(twoFactorTarget);
          return;
        }
        if (err instanceof ApiError && err.status === 401) {
          router.replace("/login");
          return;
        }
        setLoadError("Не вдалося завантажити адмін-кабінет.");
      });
  }, [router]);

  async function onLogout() {
    await logout();
    router.replace("/login");
  }

  if (!me) {
    return (
      <>
        <AppHeader context="Адміністратор" />
        <main className="flex flex-1 items-center justify-center p-8">
          <p className={loadError ? "text-danger" : "text-ink-muted"}>{loadError ?? "Завантаження…"}</p>
        </main>
      </>
    );
  }

  return (
    <>
      <AppHeader context="Адміністратор" userName={me.displayName} onLogout={onLogout} />
      <main className="flex flex-1 justify-center px-6 pt-10 pb-24">
        <div className="flex w-full max-w-260 flex-col gap-7">
          <div className="flex flex-col gap-2">
            <h1 className="text-[32px] leading-[1.15] font-medium tracking-[-0.02em]">Адмін-кабінет</h1>
            <p className="text-ink-muted text-base leading-relaxed">Керування контентом і платформою.</p>
          </div>

          <div className="grid grid-cols-[repeat(auto-fill,minmax(280px,1fr))] gap-4">
            <Link
              href="/moderation"
              className="bg-surface border-line shadow-card hover:border-brand-line text-ink flex flex-col gap-3 rounded-xl border p-5 no-underline transition-colors duration-100"
            >
              <div className="flex items-center justify-between gap-3">
                <h2 className="text-[17px] font-medium">Модерація уроків</h2>
                {queueCount !== null ? (
                  <Pill tone={queueCount > 0 ? "waiting" : "ok"}>У черзі: {queueCount}</Pill>
                ) : null}
              </div>
              <p className="text-ink-muted text-sm leading-relaxed">
                Перевірка уроків, що пройшли автоматичну валідацію.
              </p>
            </Link>
            <AdminCard
              href="/admin/lessons"
              title="Уроки"
              description="Створення уроків і генерація нових версій за допомогою ШІ."
            />
            <AdminCard
              href="/admin/audit"
              title="Аудит"
              description="Журнал генерації, модерації, озвучки та доступу до даних."
            />
            <AdminCard
              href="/admin/teachers"
              title="Вчителі"
              description="Запрошення вчителів і керування їхніми акаунтами."
            />
          </div>

          <ShellLinesVoicing />
        </div>
      </main>
    </>
  );
}

function AdminCard({ href, title, description }: { href: string; title: string; description: string }) {
  return (
    <Link
      href={href}
      className="bg-surface border-line shadow-card hover:border-brand-line text-ink flex flex-col gap-3 rounded-xl border p-5 no-underline transition-colors duration-100"
    >
      <h2 className="text-[17px] font-medium">{title}</h2>
      <p className="text-ink-muted text-sm leading-relaxed">{description}</p>
    </Link>
  );
}

/**
 * The guide's fixed child-mode lines (greeting, "turn the device", lesson end, rest) are written in
 * the code, not generated, so they skip moderation and are voiced here directly. Audited.
 */
function ShellLinesVoicing() {
  const [voicing, setVoicing] = useState(false);
  const [result, setResult] = useState<VoicingResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function onVoice() {
    setVoicing(true);
    setError(null);
    try {
      setResult(await apiFetch<VoicingResult>("/admin/kid-shell-lines/voice", { method: "POST" }));
    } catch (err) {
      setError(err instanceof ApiError && err.status === 502 ? "Сервіс озвучення недоступний." : "Не вдалося озвучити.");
    } finally {
      setVoicing(false);
    }
  }

  return (
    <section className="bg-surface border-line shadow-card flex flex-wrap items-center justify-between gap-4 rounded-xl border p-5">
      <div className="flex max-w-160 flex-col gap-1.5">
        <h2 className="text-[17px] font-medium">Репліки дитячого режиму</h2>
        <p className="text-ink-muted text-sm leading-relaxed">
          Привітання провідника, прохання повернути пристрій, свято наприкінці уроку й «сон». Тексти
          фіксовані в коді; вже озвучені репліки повторно не синтезуються.
        </p>
        {result ? (
          <p className="text-ink-soft text-sm">
            Реплік: {result.linesTotal}, нових: {result.synthesized}, вже озвучених: {result.cached}.
          </p>
        ) : null}
        {error ? <p className="text-danger text-sm">{error}</p> : null}
      </div>
      <SecondaryButton type="button" onClick={onVoice} disabled={voicing}>
        {voicing ? "Озвучуємо…" : "Озвучити репліки"}
      </SecondaryButton>
    </section>
  );
}
