"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { apiFetch, ApiError, logout } from "@/lib/api";
import type { Child, Group, Me } from "@/lib/api-types";
import { AppHeader } from "@/components/ui/AppHeader";
import { Avatar, tintFor } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { Notice, NoticeButton } from "@/components/ui/Notice";
import { Pill } from "@/components/ui/Pill";
import { SecondaryLink } from "@/components/ui/SecondaryButton";
import { Tooltip } from "@/components/ui/Tooltip";

const ROLE_LABELS: Record<Me["role"], string> = {
  PARENT: "Батьки",
  TEACHER: "Вчитель",
  ADMIN: "Адміністратор",
};

const CHILD_STATUS: Record<Child["status"], { label: string; tone: "ok" | "waiting"; meta: string }> = {
  active: { label: "Активна", tone: "ok", meta: "Профіль активний" },
  pending_consent: { label: "Очікує згоди", tone: "waiting", meta: "Запрошення надіслано" },
};

export default function DashboardPage() {
  const router = useRouter();
  const [me, setMe] = useState<Me | null>(null);
  const [children, setChildren] = useState<Child[] | null>(null);
  const [groups, setGroups] = useState<Group[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<ApiError | null>(null);
  const [resendState, setResendState] = useState<"idle" | "sending" | "sent">("idle");
  const [resendError, setResendError] = useState<ApiError | null>(null);

  // Promise-chained rather than async/await: react-hooks/set-state-in-effect flags setState
  // calls that are directly in the invoked function's body, even after an await — only calls
  // nested inside a .then()/.catch() callback (a genuinely separate, deferred closure) pass.
  const load = useCallback(() => {
    apiFetch<Me>("/auth/me")
      .then((meData) => {
        // /api/children is parent-only, /api/groups is teacher-only — admins see just the header.
        const childrenPromise = meData.role === "PARENT" ? apiFetch<Child[]>("/children") : Promise.resolve([]);
        const groupsPromise = meData.role === "TEACHER" ? apiFetch<Group[]>("/groups") : Promise.resolve([]);
        return Promise.all([childrenPromise, groupsPromise]).then(([childrenData, groupsData]) => {
          setMe(meData);
          setChildren(childrenData);
          setGroups(groupsData);
        });
      })
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) {
          router.replace("/login");
          return;
        }
        setLoadError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка"));
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
      setResendError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка"));
    }
  }

  async function onLogout() {
    await logout();
    router.push("/login");
  }

  if (loading) {
    return (
      <>
        <AppHeader />
        <main className="flex flex-1 items-center justify-center p-8">
          <p className="text-ink-muted">Завантаження…</p>
        </main>
      </>
    );
  }

  if (loadError || !me || !children || !groups) {
    return (
      <>
        <AppHeader />
        <main className="flex flex-1 items-center justify-center p-8">
          <p className="text-danger">{loadError?.message ?? "Не вдалося завантажити кабінет."}</p>
        </main>
      </>
    );
  }

  return (
    <>
      <AppHeader context={ROLE_LABELS[me.role]} userName={me.displayName} onLogout={onLogout} />

      <main className="flex flex-1 justify-center px-6 pt-10 pb-24">
        <div className="flex w-full max-w-260 flex-col gap-7">
          {!me.emailVerified ? (
            <Notice
              title="Підтвердьте email, щоб додати дитину"
              action={
                <>
                  <NoticeButton
                    type="button"
                    onClick={onResendVerification}
                    disabled={resendState !== "idle"}
                  >
                    {resendState === "sending"
                      ? "Надсилаємо…"
                      : resendState === "sent"
                        ? "Лист надіслано"
                        : "Надіслати лист повторно"}
                  </NoticeButton>
                  {resendError ? (
                    <span className="text-danger text-sm">
                      {resendError.status === 429
                        ? resendError.retryAfterSeconds
                          ? `Занадто часто. Спробуйте через ${resendError.retryAfterSeconds} с.`
                          : "Занадто часто. Спробуйте пізніше."
                        : resendError.message}
                    </span>
                  ) : null}
                </>
              }
            >
              Ми надіслали лист на {me.email}. Посилання дійсне 24 години.
            </Notice>
          ) : null}

          <div className="flex flex-col gap-2">
            <h1 className="text-[32px] leading-[1.15] font-medium tracking-[-0.02em]">
              Вітаємо, {me.displayName}
            </h1>
            <p className="text-ink-muted text-base leading-relaxed">
              Тут ви керуєте профілями дітей, переглядаєте звіти та налаштування безпеки.
            </p>
          </div>

          {me.role === "PARENT" ? (
            <section className="flex flex-col gap-4">
              <div className="flex flex-wrap items-center justify-between gap-4">
                <div className="flex items-baseline gap-2.5">
                  <h2 className="text-xl font-medium tracking-[-0.01em]">Мої діти</h2>
                  <span className="text-ink-icon text-sm">{countLabel(children.length)}</span>
                </div>

                {me.emailVerified ? (
                  <Link href="/children/new" className="no-underline">
                    <Button type="button" className="h-11 w-auto px-5 text-[15px]">
                      + Додати дитину
                    </Button>
                  </Link>
                ) : (
                  <Tooltip text="Підтвердьте email, щоб додати дитину. Кнопка стане активною після підтвердження.">
                    {(describedBy) => (
                      <button
                        type="button"
                        disabled
                        aria-describedby={describedBy}
                        className="h-11 cursor-not-allowed rounded-lg bg-[color:var(--color-ink-icon)]/55 px-5 text-[15px] font-medium text-white"
                      >
                        + Додати дитину
                      </button>
                    )}
                  </Tooltip>
                )}
              </div>

              {children.length === 0 ? (
                <p className="text-ink-muted">Ще немає жодного дитячого профілю.</p>
              ) : (
                <div className="grid grid-cols-[repeat(auto-fill,minmax(300px,1fr))] gap-4">
                  {children.map((child) => {
                    const status = CHILD_STATUS[child.status];
                    return (
                      <article
                        key={child.id}
                        className="bg-surface border-line shadow-card flex flex-col gap-4.5 rounded-xl border p-5"
                      >
                        <div className="flex items-center gap-3.5">
                          <Avatar name={child.displayName} size={52} tint={tintFor(child.id)} />
                          <div className="flex min-w-0 flex-col gap-1.5">
                            <Link
                              href={`/children/${child.id}`}
                              className="text-ink text-[17px] font-medium tracking-[-0.005em] no-underline hover:underline"
                            >
                              {child.displayName}
                            </Link>
                            <Pill tone={status.tone}>{status.label}</Pill>
                          </div>
                        </div>
                        <div className="border-line-soft text-ink-icon flex items-center justify-between gap-3 border-t pt-3.5 text-sm">
                          <span>{status.meta}</span>
                          <SecondaryLink href={`/play/${child.id}`}>
                            Кабінет дитини → грати
                          </SecondaryLink>
                        </div>
                      </article>
                    );
                  })}
                </div>
              )}
            </section>
          ) : null}

          {me.role === "TEACHER" ? (
            <section className="flex flex-col gap-4">
              <div className="flex flex-wrap items-center justify-between gap-4">
                <div className="flex items-baseline gap-2.5">
                  <h2 className="text-xl font-medium tracking-[-0.01em]">Мої групи</h2>
                  <span className="text-ink-icon text-sm">{groupCountLabel(groups.length)}</span>
                </div>
                <SecondaryLink href="/groups">Керувати групами</SecondaryLink>
              </div>

              {groups.length === 0 ? (
                <p className="text-ink-muted">Ще немає жодної групи.</p>
              ) : (
                <div className="grid grid-cols-[repeat(auto-fill,minmax(300px,1fr))] gap-4">
                  {groups.map((group) => (
                    <article
                      key={group.id}
                      className="bg-surface border-line shadow-card flex flex-col gap-4.5 rounded-xl border p-5"
                    >
                      <div className="flex items-center gap-3.5">
                        <Avatar name={group.name} size={52} tint={tintFor(group.id)} />
                        <div className="flex min-w-0 flex-col gap-1.5">
                          <Link
                            href={`/groups/${group.id}`}
                            className="text-ink text-[17px] font-medium tracking-[-0.005em] no-underline hover:underline"
                          >
                            {group.name}
                          </Link>
                          <Pill tone={group.isActive ? "ok" : "neutral"}>
                            {group.isActive ? "Активна" : "Архів"}
                          </Pill>
                        </div>
                      </div>
                      <div className="border-line-soft text-ink-icon flex items-center justify-between gap-3 border-t pt-3.5 text-sm">
                        <span className="font-mono">Код: {group.joinCode}</span>
                        <SecondaryLink href={`/groups/${group.id}`}>Відкрити</SecondaryLink>
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </section>
          ) : null}
        </div>
      </main>
    </>
  );
}

/** «1 профіль / 3 профілі / 5 профілів» — інакше підпис під заголовком читається як помилка. */
function countLabel(n: number): string {
  return `${n} ${plural(n, "профіль", "профілі", "профілів")}`;
}

function groupCountLabel(n: number): string {
  return `${n} ${plural(n, "група", "групи", "груп")}`;
}

function plural(n: number, one: string, few: string, many: string): string {
  const mod100 = n % 100;
  const mod10 = n % 10;
  if (mod100 >= 11 && mod100 <= 14) return many;
  if (mod10 === 1) return one;
  if (mod10 >= 2 && mod10 <= 4) return few;
  return many;
}
