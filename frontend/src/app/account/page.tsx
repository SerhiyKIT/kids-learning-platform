"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { apiFetch, ApiError, logout } from "@/lib/api";
import type { DeletionPreview, Me } from "@/lib/api-types";
import { downloadExport } from "@/lib/download";
import { PASSWORD_TOO_LONG_TEXT, passwordTooLong } from "@/lib/password";
import { AppHeader } from "@/components/ui/AppHeader";
import { Button } from "@/components/ui/Button";
import { Section, SectionHeader } from "@/components/ui/Card";
import { Field, PasswordInput, TextInput } from "@/components/ui/Field";
import { Modal } from "@/components/ui/Modal";
import { DangerButton, QuietButton, SecondaryButton } from "@/components/ui/SecondaryButton";

const CONFIRM_WORD = "ВИДАЛИТИ";

/**
 * «Мій акаунт» (docs/Ролі_та_приватність.md §1–§3): change the password, sign out on every device,
 * download all my data, and — for parents — delete the account for real.
 */
export default function AccountPage() {
  const router = useRouter();
  const [me, setMe] = useState<Me | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [deleted, setDeleted] = useState(false);

  useEffect(() => {
    apiFetch<Me>("/auth/me")
      .then(setMe)
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) {
          router.replace("/login");
          return;
        }
        setLoadError("Не вдалося завантажити акаунт.");
      });
  }, [router]);

  async function onLogout() {
    await logout();
    router.replace("/login");
  }

  if (deleted) {
    return (
      <>
        <AppHeader />
        <main className="flex flex-1 flex-col items-center justify-center gap-4 p-8 text-center">
          <h1 className="text-[26px] font-medium">Акаунт видалено</h1>
          <p className="text-ink-muted max-w-110 text-pretty">
            Ваш акаунт і дані дітей, у яких не було інших батьків, видалено назавжди. Ви вийшли з системи.
          </p>
          <Link href="/" className="text-sm">
            На головну
          </Link>
        </main>
      </>
    );
  }

  if (!me) {
    return (
      <>
        <AppHeader />
        <main className="flex flex-1 items-center justify-center p-8">
          <p className={loadError ? "text-danger" : "text-ink-muted"}>{loadError ?? "Завантаження…"}</p>
        </main>
      </>
    );
  }

  return (
    <>
      <AppHeader userName={me.displayName} onLogout={onLogout} />
      <main className="flex flex-1 justify-center px-6 pt-7 pb-24">
        <div className="flex w-full max-w-220 flex-col gap-6">
          <Link href="/dashboard" className="text-ink-muted text-sm no-underline hover:underline">
            ← До кабінету
          </Link>
          <div className="flex flex-col gap-1.5">
            <h1 className="text-[30px] leading-[1.15] font-medium tracking-[-0.02em]">Мій акаунт</h1>
            <p className="text-ink-muted">{me.email}</p>
          </div>

          <PasswordSection />
          <SessionsSection onDone={() => router.replace("/login")} />
          <ExportSection />
          {me.role === "PARENT" ? (
            <DeleteSection onDeleted={() => setDeleted(true)} />
          ) : (
            <Section>
              <SectionHeader
                title="Видалення акаунта"
                description={
                  me.role === "TEACHER"
                    ? "Акаунт вчителя видаляє адміністратор — зверніться до нього."
                    : "Акаунт адміністратора не можна видалити самостійно."
                }
              />
            </Section>
          )}
        </div>
      </main>
    </>
  );
}

function passwordErrorText(err: unknown, wrongPasswordText: string): string {
  if (err instanceof ApiError) {
    if (err.status === 401) return wrongPasswordText;
    if (err.status === 429) {
      return err.retryAfterSeconds
        ? `Занадто багато спроб. Спробуйте за ${err.retryAfterSeconds} с.`
        : "Занадто багато спроб. Спробуйте пізніше.";
    }
    if (err.status === 400) return "Новий пароль має містити щонайменше 10 символів.";
  }
  return "Не вдалося виконати дію. Спробуйте ще раз.";
}

function PasswordSection() {
  const [current, setCurrent] = useState("");
  const [next, setNext] = useState("");
  const [repeat, setRepeat] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setDone(false);
    if (next !== repeat) {
      setError("Нові паролі не збігаються.");
      return;
    }
    if (passwordTooLong(next)) {
      setError(PASSWORD_TOO_LONG_TEXT);
      return;
    }
    setSaving(true);
    try {
      await apiFetch("/me/password", { method: "POST", body: JSON.stringify({ currentPassword: current, newPassword: next }) });
      setCurrent("");
      setNext("");
      setRepeat("");
      setDone(true);
    } catch (err) {
      setError(passwordErrorText(err, "Невірний поточний пароль."));
    } finally {
      setSaving(false);
    }
  }

  return (
    <Section>
      <SectionHeader
        title="Пароль"
        description="Після зміни пароля всі інші пристрої вийдуть з акаунта; цей лишиться в системі."
      />
      <form onSubmit={onSubmit} className="flex max-w-110 flex-col gap-4 px-6 pt-1 pb-6">
        <Field label="Поточний пароль">
          {({ id }) => (
            <PasswordInput id={id} required autoComplete="current-password" value={current} onChange={(e) => setCurrent(e.target.value)} />
          )}
        </Field>
        <Field label="Новий пароль (щонайменше 10 символів)">
          {({ id }) => (
            <PasswordInput id={id} required minLength={10} autoComplete="new-password" value={next} onChange={(e) => setNext(e.target.value)} />
          )}
        </Field>
        <Field label="Повторіть новий пароль">
          {({ id }) => (
            <PasswordInput id={id} required minLength={10} autoComplete="new-password" value={repeat} onChange={(e) => setRepeat(e.target.value)} />
          )}
        </Field>
        {error ? <p role="alert" className="text-danger text-sm">{error}</p> : null}
        {done ? <p role="status" className="text-mark-ok text-sm">Пароль змінено. Інші пристрої вийшли з акаунта.</p> : null}
        <Button type="submit" disabled={saving} className="h-11 w-auto self-start px-5 text-[15px]">
          {saving ? "Зберігаємо…" : "Змінити пароль"}
        </Button>
      </form>
    </Section>
  );
}

function SessionsSection({ onDone }: { onDone: () => void }) {
  const [open, setOpen] = useState(false);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function onConfirm() {
    setWorking(true);
    setError(null);
    try {
      await apiFetch("/me/logout-everywhere", { method: "POST" });
      onDone();
    } catch {
      setError("Не вдалося вийти на всіх пристроях. Спробуйте ще раз.");
      setWorking(false);
    }
  }

  return (
    <Section>
      <SectionHeader
        title="Пристрої"
        description="Якщо ви входили з чужого пристрою або загубили телефон — завершіть усі сеанси, включно з цим."
      />
      <div className="px-6 pt-1 pb-6">
        <SecondaryButton type="button" onClick={() => setOpen(true)}>
          Вийти на всіх пристроях
        </SecondaryButton>
      </div>
      {open ? (
        <Modal
          title="Вийти на всіх пристроях?"
          onClose={() => setOpen(false)}
          footer={
            <>
              <QuietButton type="button" className="h-11 px-4 text-[15px]" onClick={() => setOpen(false)}>
                Скасувати
              </QuietButton>
              <Button type="button" className="h-11 w-auto px-5 text-[15px]" disabled={working} onClick={() => void onConfirm()}>
                {working ? "Виходимо…" : "Вийти скрізь"}
              </Button>
            </>
          }
        >
          <p className="text-ink-soft text-sm leading-relaxed text-pretty">
            Усі пристрої, зокрема цей і дитячий режим на планшеті, вийдуть з акаунта. Щоб продовжити, увійдіть знову.
          </p>
          {error ? <p className="text-danger text-sm">{error}</p> : null}
        </Modal>
      ) : null}
    </Section>
  );
}

function ExportSection() {
  const [working, setWorking] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function onDownload() {
    setWorking(true);
    setError(null);
    try {
      await downloadExport("/me/export", "kidlearn-account.json");
    } catch {
      setError("Не вдалося підготувати файл. Спробуйте ще раз.");
    } finally {
      setWorking(false);
    }
  }

  return (
    <Section>
      <SectionHeader
        title="Мої дані"
        description="Файл JSON з усім, що система знає про ваш акаунт і ваших дітей: профілі, згоди, групи, усі спроби уроків з відповідями, налаштування гри та оформлення."
      />
      <div className="flex flex-col gap-2 px-6 pt-1 pb-6">
        <SecondaryButton type="button" onClick={() => void onDownload()} disabled={working} className="self-start">
          {working ? "Готуємо файл…" : "Завантажити всі мої дані"}
        </SecondaryButton>
        {error ? <p className="text-danger text-sm">{error}</p> : null}
      </div>
    </Section>
  );
}

function DeleteSection({ onDeleted }: { onDeleted: () => void }) {
  const [preview, setPreview] = useState<DeletionPreview | null>(null);
  const [open, setOpen] = useState(false);
  const [password, setPassword] = useState("");
  const [word, setWord] = useState("");
  const [working, setWorking] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiFetch<DeletionPreview>("/me/deletion-preview")
      .then(setPreview)
      .catch(() => setPreview({ children: [] }));
  }, []);

  const removed = preview?.children.filter((c) => c.deletedCompletely) ?? [];
  const kept = preview?.children.filter((c) => !c.deletedCompletely) ?? [];

  async function onDelete() {
    setWorking(true);
    setError(null);
    try {
      await apiFetch("/me", { method: "DELETE", body: JSON.stringify({ password }) });
      onDeleted();
    } catch (err) {
      setError(passwordErrorText(err, "Невірний пароль."));
      setPassword("");
      setWorking(false);
    }
  }

  return (
    <section className="bg-surface border-danger-line-soft flex flex-col gap-4 rounded-xl border px-6 py-5.5">
      <div className="flex flex-col gap-1.5">
        <h2 className="text-danger-title text-lg font-medium">Видалити акаунт</h2>
        <p className="text-ink-soft text-sm leading-relaxed text-pretty">
          Це остаточно і не скасовується. Ваш акаунт (email, ім&apos;я, пароль) видаляється повністю, а не
          приховується. Записи журналу дій лишаються без прив&apos;язки до вас.
        </p>
      </div>
      {preview ? <ChildFates removed={removed} kept={kept} /> : <p className="text-ink-muted text-sm">Завантаження…</p>}
      <div>
        <DangerButton type="button" onClick={() => setOpen(true)} disabled={!preview}>
          Видалити акаунт…
        </DangerButton>
      </div>

      {open ? (
        <Modal
          title="Видалити акаунт назавжди?"
          onClose={() => {
            setOpen(false);
            setPassword("");
            setWord("");
            setError(null);
          }}
          footer={
            <>
              <QuietButton type="button" className="h-11 px-4 text-[15px]" onClick={() => setOpen(false)}>
                Скасувати
              </QuietButton>
              <DangerButton type="button" onClick={() => void onDelete()} disabled={working || password === "" || word !== CONFIRM_WORD}>
                {working ? "Видаляємо…" : "Видалити назавжди"}
              </DangerButton>
            </>
          }
        >
          <ChildFates removed={removed} kept={kept} />
          <Field label="Ваш пароль">
            {({ id }) => (
              <PasswordInput id={id} autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} />
            )}
          </Field>
          <Field label={`Введіть слово «${CONFIRM_WORD}» для підтвердження`}>
            {({ id }) => <TextInput id={id} value={word} onChange={(e) => setWord(e.target.value)} placeholder={CONFIRM_WORD} />}
          </Field>
          {error ? <p role="alert" className="text-danger text-sm">{error}</p> : null}
        </Modal>
      ) : null}
    </section>
  );
}

function ChildFates({
  removed,
  kept,
}: {
  removed: DeletionPreview["children"];
  kept: DeletionPreview["children"];
}) {
  return (
    <div className="flex flex-col gap-2 text-sm leading-relaxed">
      {removed.length > 0 ? (
        <div data-fate="deleted">
          <p className="font-medium">Буде видалено повністю (інших батьків немає):</p>
          <ul className="text-ink-soft list-disc pl-5">
            {removed.map((c) => (
              <li key={c.childId}>
                {c.displayName} — профіль, згоди, групи, уся історія уроків і відповідей, налаштування гри та оформлення
              </li>
            ))}
          </ul>
        </div>
      ) : null}
      {kept.length > 0 ? (
        <div data-fate="kept">
          <p className="font-medium">Лишаться в іншого батька (зникне лише ваш зв&apos;язок з дитиною):</p>
          <ul className="text-ink-soft list-disc pl-5">
            {kept.map((c) => (
              <li key={c.childId}>{c.displayName}</li>
            ))}
          </ul>
        </div>
      ) : null}
      {removed.length === 0 && kept.length === 0 ? <p className="text-ink-soft">Профілів дітей у вас немає.</p> : null}
    </div>
  );
}
