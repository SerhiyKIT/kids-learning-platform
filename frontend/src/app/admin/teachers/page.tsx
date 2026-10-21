"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { apiFetch, ApiError, logout } from "@/lib/api";
import {
  TEACHER_STATUS_LABELS,
  type InvitedTeacher,
  type InviteTeacherRequest,
  type TeacherEntry,
} from "@/lib/admin-types";
import { adminRedirect } from "@/lib/two-factor";
import { AppHeader } from "@/components/ui/AppHeader";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Section, SectionHeader } from "@/components/ui/Card";
import { Field, TextInput } from "@/components/ui/Field";
import { Modal } from "@/components/ui/Modal";
import { Pill } from "@/components/ui/Pill";
import { DangerButton, QuietButton, SecondaryButton, SecondaryLink } from "@/components/ui/SecondaryButton";

/**
 * Teacher administration: invite (the teacher sets their own password from the emailed link — the
 * admin never sees or sets it), re-send a pending invitation, deactivate. Shows only the teachers'
 * own account fields and their group count — no child or parent data.
 */
export default function AdminTeachersPage() {
  const router = useRouter();
  const [teachers, setTeachers] = useState<TeacherEntry[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [inviteOpen, setInviteOpen] = useState(false);
  const [deactivating, setDeactivating] = useState<TeacherEntry | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [rowError, setRowError] = useState<{ id: string; text: string } | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  const handleError = useCallback(
    (err: unknown): boolean => {
      const target = adminRedirect(err);
      if (target) {
        router.replace(target);
        return true;
      }
      return false;
    },
    [router],
  );

  const load = useCallback(() => {
    apiFetch<TeacherEntry[]>("/admin/teachers")
      .then((data) => {
        setTeachers(data);
        setLoadError(null);
      })
      .catch((err) => {
        if (!handleError(err)) setLoadError("Не вдалося завантажити вчителів.");
      });
  }, [handleError]);

  useEffect(() => {
    load();
  }, [load]);

  async function onResend(teacher: TeacherEntry) {
    setBusyId(teacher.id);
    setRowError(null);
    setNotice(null);
    try {
      await apiFetch(`/admin/teachers/${teacher.id}/resend-invite`, { method: "POST" });
      setNotice(`Нове запрошення надіслано на ${teacher.email}. Попереднє посилання більше не діє.`);
    } catch (err) {
      if (handleError(err)) return;
      setRowError({
        id: teacher.id,
        text:
          err instanceof ApiError && err.code === "INVITE_ALREADY_ACCEPTED"
            ? "Вчитель уже прийняв запрошення."
            : "Не вдалося надіслати запрошення.",
      });
      load();
    } finally {
      setBusyId(null);
    }
  }

  async function onDeactivateConfirmed(teacher: TeacherEntry) {
    setBusyId(teacher.id);
    setRowError(null);
    setNotice(null);
    try {
      await apiFetch(`/admin/teachers/${teacher.id}/deactivate`, { method: "POST" });
      setDeactivating(null);
      setNotice(`${teacher.displayName} деактивовано. Групи й результати дітей збережено.`);
      load();
    } catch (err) {
      if (handleError(err)) return;
      setDeactivating(null);
      setRowError({ id: teacher.id, text: "Не вдалося деактивувати вчителя." });
    } finally {
      setBusyId(null);
    }
  }

  async function onLogout() {
    await logout();
    router.replace("/login");
  }

  return (
    <>
      <AppHeader context="Адміністратор" onLogout={onLogout} />
      <main className="flex flex-1 justify-center px-6 pt-10 pb-24">
        <div className="flex w-full max-w-260 flex-col gap-7">
          <div className="flex flex-wrap items-end justify-between gap-4">
            <div className="flex flex-col gap-2">
              <SecondaryLink href="/admin">← Адмін-кабінет</SecondaryLink>
              <h1 className="text-[32px] leading-[1.15] font-medium tracking-[-0.02em]">Вчителі</h1>
              <p className="text-ink-muted text-base leading-relaxed">
                Запрошений вчитель отримає лист і сам створить пароль.
              </p>
            </div>
            <Button type="button" onClick={() => setInviteOpen(true)} className="h-11 w-auto px-5 text-[15px]">
              + Запросити вчителя
            </Button>
          </div>

          {notice ? <p className="text-ok-ink text-sm">{notice}</p> : null}

          <Section>
            <SectionHeader title="Усі вчителі" aside={teachers ? String(teachers.length) : undefined} />
            {loadError ? <p className="text-danger px-6 py-5 text-sm">{loadError}</p> : null}
            {!teachers && !loadError ? <p className="text-ink-muted px-6 py-5 text-sm">Завантаження…</p> : null}
            {teachers && teachers.length === 0 ? (
              <p className="text-ink-muted px-6 py-5 text-sm">Ще немає жодного вчителя.</p>
            ) : null}
            {teachers && teachers.length > 0 ? (
              <div className="overflow-x-auto">
                <table className="w-full min-w-[760px] text-left text-sm">
                  <thead className="bg-row-hover text-ink-faint font-mono text-xs tracking-[0.08em] uppercase">
                    <tr>
                      <th className="px-6 py-3 font-normal">Ім&apos;я</th>
                      <th className="px-3 py-3 font-normal">Email</th>
                      <th className="px-3 py-3 font-normal">Статус</th>
                      <th className="px-3 py-3 font-normal">Груп</th>
                      <th className="px-3 py-3 font-normal">Додано</th>
                      <th className="px-6 py-3 font-normal" />
                    </tr>
                  </thead>
                  <tbody>
                    {teachers.map((teacher) => {
                      const status = TEACHER_STATUS_LABELS[teacher.status];
                      return (
                        <tr key={teacher.id} className="border-line-row border-t align-top">
                          <td className="px-6 py-3.5 text-[15px] font-medium">{teacher.displayName}</td>
                          <td className="text-ink-muted px-3 py-3.5">{teacher.email}</td>
                          <td className="px-3 py-3.5">
                            <Pill tone={status.tone}>{status.label}</Pill>
                          </td>
                          <td className="px-3 py-3.5 font-mono">{teacher.groupCount}</td>
                          <td className="text-ink-icon px-3 py-3.5 whitespace-nowrap">
                            {new Date(teacher.createdAt).toLocaleDateString("uk-UA")}
                          </td>
                          <td className="px-6 py-3">
                            <div className="flex flex-wrap justify-end gap-2">
                              {teacher.status === "invited" ? (
                                <SecondaryButton
                                  type="button"
                                  disabled={busyId !== null}
                                  onClick={() => onResend(teacher)}
                                  className="disabled:opacity-50"
                                >
                                  {busyId === teacher.id ? "Надсилаємо…" : "Надіслати запрошення ще раз"}
                                </SecondaryButton>
                              ) : null}
                              {teacher.status !== "deactivated" ? (
                                <QuietButton
                                  type="button"
                                  disabled={busyId !== null}
                                  onClick={() => setDeactivating(teacher)}
                                >
                                  Деактивувати
                                </QuietButton>
                              ) : null}
                            </div>
                            {rowError?.id === teacher.id ? (
                              <p className="text-danger mt-2 text-right text-[13px]">{rowError.text}</p>
                            ) : null}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            ) : null}
          </Section>
        </div>
      </main>

      {inviteOpen ? (
        <InviteModal
          onClose={() => setInviteOpen(false)}
          onInvited={(teacher) => {
            setInviteOpen(false);
            setNotice(`Запрошення надіслано на ${teacher.email}.`);
            load();
          }}
          onError={handleError}
        />
      ) : null}

      {deactivating ? (
        <Modal
          title={`Деактивувати ${deactivating.displayName}?`}
          onClose={() => setDeactivating(null)}
          footer={
            <>
              <QuietButton type="button" onClick={() => setDeactivating(null)} className="h-11 px-4.5 text-[15px]">
                Скасувати
              </QuietButton>
              <DangerButton
                type="button"
                disabled={busyId !== null}
                onClick={() => onDeactivateConfirmed(deactivating)}
              >
                {busyId === deactivating.id ? "Деактивуємо…" : "Деактивувати"}
              </DangerButton>
            </>
          }
        >
          <p className="text-ink-soft text-[15px] leading-relaxed">
            Вчитель втратить доступ. Групи й результати дітей збережуться.
          </p>
        </Modal>
      ) : null}
    </>
  );
}

function InviteModal({
  onClose,
  onInvited,
  onError,
}: {
  onClose: () => void;
  onInvited: (teacher: InvitedTeacher) => void;
  onError: (err: unknown) => boolean;
}) {
  const [email, setEmail] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (submitting) return;
    setSubmitting(true);
    setError(null);
    const body: InviteTeacherRequest = { email: email.trim(), displayName: displayName.trim() };
    try {
      onInvited(await apiFetch<InvitedTeacher>("/admin/teachers", { method: "POST", body: JSON.stringify(body) }));
    } catch (err) {
      if (onError(err)) return;
      if (err instanceof ApiError && err.code === "EMAIL_TAKEN") {
        setError("Цей email уже використовується.");
      } else if (err instanceof ApiError && err.status === 400) {
        setError("Перевірте email та ім'я.");
      } else {
        setError("Не вдалося надіслати запрошення.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal
      title="Запросити вчителя"
      onClose={onClose}
      footer={
        <>
          <QuietButton type="button" onClick={onClose} className="h-11 px-4.5 text-[15px]">
            Скасувати
          </QuietButton>
          <Button
            type="submit"
            form="invite-teacher"
            disabled={submitting || !email.trim() || !displayName.trim()}
            className="h-11 w-auto px-5 text-[15px]"
          >
            {submitting ? "Надсилаємо…" : "Надіслати запрошення"}
          </Button>
        </>
      }
    >
      <form id="invite-teacher" onSubmit={onSubmit} className="flex flex-col gap-4">
        {error ? <Alert title="Помилка">{error}</Alert> : null}
        <Field label="Email">
          {({ id }) => (
            <TextInput
              id={id}
              type="email"
              required
              autoFocus
              placeholder="teacher@school.ua"
              invalid={error !== null}
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          )}
        </Field>
        <Field label="Ім'я">
          {({ id }) => (
            <TextInput
              id={id}
              required
              maxLength={120}
              placeholder="Олена Петрівна"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
            />
          )}
        </Field>
        <p className="text-ink-soft text-[13px] leading-snug">
          Пароль вчитель створить сам за посиланням з листа. Посилання діє 72 години.
        </p>
      </form>
    </Modal>
  );
}
