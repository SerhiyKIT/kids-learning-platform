"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { apiFetch, ApiError, logout } from "@/lib/api";
import type { PendingReviewEntry } from "@/lib/admin-types";
import type { Me } from "@/lib/api-types";
import { twoFactorRedirect } from "@/lib/two-factor";
import { AppHeader } from "@/components/ui/AppHeader";
import { Pill } from "@/components/ui/Pill";

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
