"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { apiFetch, ApiError, logout } from "@/lib/api";
import type { Child, Me } from "@/lib/api-types";

const ROLE_LABELS: Record<Me["role"], string> = {
  PARENT: "Батьки",
  TEACHER: "Вчитель",
  ADMIN: "Адміністратор",
};

const STATUS_BADGES: Record<Child["status"], { label: string; className: string }> = {
  pending_consent: { label: "Очікує згоди", className: "bg-amber-100 text-amber-800" },
  active: { label: "Активна", className: "bg-emerald-100 text-emerald-800" },
};

export default function DashboardPage() {
  const router = useRouter();
  const [me, setMe] = useState<Me | null>(null);
  const [children, setChildren] = useState<Child[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<ApiError | null>(null);
  const [resendState, setResendState] = useState<"idle" | "sending" | "sent">("idle");
  const [resendError, setResendError] = useState<ApiError | null>(null);

  // Promise-chained rather than async/await: react-hooks/set-state-in-effect flags setState
  // calls that are directly in the invoked function's body, even after an await — only calls
  // nested inside a .then()/.catch() callback (a genuinely separate, deferred closure) pass.
  const load = useCallback(() => {
    apiFetch<Me>("/auth/me")
      .then((meData) =>
        // /api/children is parent-only; teacher/admin logins skip it and just see the header.
        (meData.role === "PARENT" ? apiFetch<Child[]>("/children") : Promise.resolve([])).then(
          (childrenData) => {
            setMe(meData);
            setChildren(childrenData);
          },
        ),
      )
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) {
          router.replace("/login");
          return;
        }
        setLoadError(err instanceof ApiError ? err : new ApiError(0, "Unexpected error"));
      })
      .finally(() => setLoading(false));
  }, [router]);

  useEffect(() => {
    load();
  }, [load]);

  async function onResendVerification() {
    setResendState("sending");
    setResendError(null);
    try {
      await apiFetch("/auth/resend-verification", { method: "POST" });
      setResendState("sent");
    } catch (err) {
      setResendState("idle");
      setResendError(err instanceof ApiError ? err : new ApiError(0, "Unexpected error"));
    }
  }

  async function onLogout() {
    await logout();
    router.push("/login");
  }

  if (loading) {
    return (
      <main className="flex flex-1 items-center justify-center p-8">
        <p>Завантаження…</p>
      </main>
    );
  }

  if (loadError || !me || !children) {
    return (
      <main className="flex flex-1 items-center justify-center p-8">
        <p className="text-red-600">{loadError?.message ?? "Не вдалося завантажити кабінет."}</p>
      </main>
    );
  }

  return (
    <main className="mx-auto flex w-full max-w-2xl flex-1 flex-col gap-6 p-6 sm:p-8">
      <header className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold">Привіт, {me.displayName}!</h1>
          <p className="text-sm text-black/60 dark:text-white/60">{ROLE_LABELS[me.role]}</p>
        </div>
        <button
          type="button"
          onClick={onLogout}
          className="shrink-0 rounded-full border border-black/[.16] px-4 py-2 text-sm dark:border-white/[.2]"
        >
          Вийти
        </button>
      </header>

      {!me.emailVerified ? (
        <div className="flex flex-col gap-2 rounded-2xl border border-amber-300 bg-amber-50 p-4 text-amber-900">
          <p className="font-medium">Підтвердьте email, щоб додати дитину</p>
          <p className="text-sm">
            Ми надіслали лист на {me.email}. Перейдіть за посиланням у листі, щоб підтвердити адресу.
          </p>
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={onResendVerification}
              disabled={resendState !== "idle"}
              className="self-start rounded-full bg-amber-600 px-4 py-1.5 text-sm text-white disabled:opacity-60"
            >
              {resendState === "sending" ? "Надсилаємо…" : resendState === "sent" ? "Лист надіслано" : "Надіслати ще раз"}
            </button>
            {resendError ? (
              <span className="text-sm text-red-700">
                {resendError.status === 429
                  ? resendError.retryAfterSeconds
                    ? `Занадто часто. Спробуйте через ${resendError.retryAfterSeconds}с.`
                    : "Занадто часто. Спробуйте пізніше."
                  : resendError.message}
              </span>
            ) : null}
          </div>
        </div>
      ) : null}

      {me.role === "PARENT" ? (
        <>
          <section className="flex flex-col gap-3">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold">Діти</h2>
              {me.emailVerified ? (
                <Link
                  href="/children/new"
                  className="rounded-full bg-blue-600 px-4 py-1.5 text-sm text-white"
                >
                  Додати дитину
                </Link>
              ) : (
                <span
                  title="Спершу підтвердьте email — див. повідомлення вище"
                  className="cursor-not-allowed rounded-full bg-black/10 px-4 py-1.5 text-sm text-black/40 dark:bg-white/10 dark:text-white/40"
                >
                  Додати дитину
                </span>
              )}
            </div>

            {children.length === 0 ? (
              <p className="text-black/60 dark:text-white/60">Ще немає жодної дитини.</p>
            ) : (
              <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
                {children.map((child) => {
                  const badge = STATUS_BADGES[child.status];
                  return (
                    <Link
                      key={child.id}
                      href={`/children/${child.id}`}
                      className="flex flex-col items-center gap-2 rounded-2xl border border-black/10 bg-white p-4 text-center shadow-sm dark:border-white/10 dark:bg-white/5"
                    >
                      <span className="text-4xl">🧒</span>
                      <span className="font-medium text-slate-800 dark:text-white">{child.displayName}</span>
                      <span className={`rounded-full px-2 py-0.5 text-xs ${badge.className}`}>{badge.label}</span>
                    </Link>
                  );
                })}
              </div>
            )}
          </section>

          <Link href="/play" className="self-start rounded-full border border-black/[.16] px-5 py-2 dark:border-white/[.2]">
            Кабінет дитини — грати
          </Link>
        </>
      ) : null}
    </main>
  );
}
