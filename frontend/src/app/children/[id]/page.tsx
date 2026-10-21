"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { apiFetch, ApiError } from "@/lib/api";
import type { Child, ConsentEntry, Group, HistoryEntry, PlaySettingsRequest, PlayState } from "@/lib/api-types";
import { AppHeader } from "@/components/ui/AppHeader";
import { Avatar, tintFor } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";
import { Pill } from "@/components/ui/Pill";
import { Section, SectionHeader } from "@/components/ui/Card";
import { DangerButton, QuietButton, SecondaryButton, SecondaryLink } from "@/components/ui/SecondaryButton";
import { TextInput } from "@/components/ui/Field";
import { COLOR_LABELS, COLOR_SPOTS, MOOD_LABELS, WORLD_LABELS, type StyleResponse } from "@/lib/theme-catalog";

const STATUS: Record<Child["status"], { label: string; tone: "ok" | "waiting" }> = {
  pending_consent: { label: "Очікує згоди", tone: "waiting" },
  active: { label: "Активна", tone: "ok" },
};

const SESSION_LIMITS: { value: PlaySettingsRequest["sessionLimitMinutes"]; label: string }[] = [
  { value: 10, label: "10 хв" },
  { value: 15, label: "15 хв" },
  { value: 20, label: "20 хв" },
  { value: 30, label: "30 хв" },
  { value: null, label: "Без ліміту" },
];

const RESULTS: Record<string, { label: string; tone: "ok" | "bad" }> = {
  completed: { label: "Завершено", tone: "ok" },
  abandoned: { label: "Перервано", tone: "bad" },
};

export default function ChildPage() {
  const { id } = useParams<{ id: string }>();

  const [child, setChild] = useState<Child | null>(null);
  const [childError, setChildError] = useState<ApiError | null>(null);
  const [childLoading, setChildLoading] = useState(true);
  const [consenting, setConsenting] = useState(false);

  const [groups, setGroups] = useState<Group[] | null>(null);
  const [groupsError, setGroupsError] = useState<ApiError | null>(null);
  const [joinCode, setJoinCode] = useState("");
  const [joining, setJoining] = useState(false);
  const [joinError, setJoinError] = useState<ApiError | null>(null);
  const [leavingGroupId, setLeavingGroupId] = useState<string | null>(null);

  const [history, setHistory] = useState<HistoryEntry[] | null>(null);
  const [historyError, setHistoryError] = useState<ApiError | null>(null);
  const [expandedAttemptId, setExpandedAttemptId] = useState<string | null>(null);

  const [playState, setPlayState] = useState<PlayState | null>(null);
  const [playError, setPlayError] = useState<ApiError | null>(null);
  const [savingLimit, setSavingLimit] = useState(false);
  const [waking, setWaking] = useState(false);

  const [style, setStyle] = useState<StyleResponse | null>(null);
  const [styleConsent, setStyleConsent] = useState<ConsentEntry | null>(null);
  const [styleError, setStyleError] = useState<ApiError | null>(null);
  const [styleSaving, setStyleSaving] = useState(false);
  const [revokeOpen, setRevokeOpen] = useState(false);

  const [confirmOpen, setConfirmOpen] = useState(false);
  const [confirmText, setConfirmText] = useState("");
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<ApiError | null>(null);
  const [deleted, setDeleted] = useState(false);

  // Also called after consent/join/leave/delete actions to refresh, not just on mount — the
  // reset-then-fetch state changes happen inside the promise callbacks (not synchronously here,
  // which react-hooks/set-state-in-effect flags), so a later refetch doesn't flash the
  // full-page "Завантаження…" state, it just quietly replaces the data in place.
  const loadChild = useCallback(() => {
    apiFetch<Child>(`/children/${id}`)
      .then((data) => {
        setChild(data);
        setChildError(null);
      })
      .catch((err) => setChildError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка")))
      .finally(() => setChildLoading(false));
  }, [id]);

  const loadGroups = useCallback(() => {
    apiFetch<Group[]>(`/children/${id}/groups`)
      .then(setGroups)
      .catch((err) => setGroupsError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка")));
  }, [id]);

  const loadHistory = useCallback(() => {
    apiFetch<HistoryEntry[]>(`/children/${id}/history`)
      .then(setHistory)
      .catch((err) => setHistoryError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка")));
  }, [id]);

  const loadPlayState = useCallback(() => {
    apiFetch<PlayState>(`/children/${id}/play-state`)
      .then((data) => {
        setPlayState(data);
        setPlayError(null);
      })
      .catch((err) => setPlayError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка")));
  }, [id]);

  const loadStyle = useCallback(() => {
    Promise.all([apiFetch<StyleResponse>(`/children/${id}/style`), apiFetch<ConsentEntry[]>(`/children/${id}/consents`)])
      .then(([styleData, consents]) => {
        setStyle(styleData);
        // The most recent style_personalization record: when it was given and, if so, revoked.
        setStyleConsent(consents.filter((c) => c.type === "style_personalization").at(-1) ?? null);
        setStyleError(null);
      })
      .catch((err) => setStyleError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка")));
  }, [id]);

  useEffect(() => {
    loadChild();
    loadGroups();
    loadHistory();
    loadPlayState();
    loadStyle();
  }, [loadChild, loadGroups, loadHistory, loadPlayState, loadStyle]);

  async function onStyleConsent(grant: boolean) {
    setStyleSaving(true);
    setStyleError(null);
    try {
      await apiFetch(`/children/${id}/consents/style_personalization`, { method: grant ? "PUT" : "DELETE" });
      setRevokeOpen(false);
      loadStyle();
    } catch (err) {
      setStyleError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка"));
    } finally {
      setStyleSaving(false);
    }
  }

  async function onSetLimit(sessionLimitMinutes: PlaySettingsRequest["sessionLimitMinutes"]) {
    setSavingLimit(true);
    setPlayError(null);
    try {
      const body: PlaySettingsRequest = { sessionLimitMinutes };
      setPlayState(await apiFetch<PlayState>(`/children/${id}/play-settings`, { method: "PUT", body: JSON.stringify(body) }));
    } catch (err) {
      setPlayError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка"));
    } finally {
      setSavingLimit(false);
    }
  }

  async function onWake() {
    setWaking(true);
    setPlayError(null);
    try {
      setPlayState(await apiFetch<PlayState>(`/children/${id}/play-state/wake`, { method: "POST" }));
    } catch (err) {
      setPlayError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка"));
    } finally {
      setWaking(false);
    }
  }

  async function onConsent() {
    setConsenting(true);
    try {
      await apiFetch(`/children/${id}/consent`, { method: "POST" });
      loadChild();
    } catch (err) {
      setChildError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка"));
    } finally {
      setConsenting(false);
    }
  }

  async function onJoinGroup(e: React.FormEvent) {
    e.preventDefault();
    setJoining(true);
    setJoinError(null);
    try {
      await apiFetch("/groups/join", {
        method: "POST",
        body: JSON.stringify({ joinCode, childId: id }),
      });
      setJoinCode("");
      loadGroups();
    } catch (err) {
      setJoinError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка"));
    } finally {
      setJoining(false);
    }
  }

  async function onLeaveGroup(groupId: string) {
    setLeavingGroupId(groupId);
    try {
      await apiFetch(`/children/${id}/groups/${groupId}`, { method: "DELETE" });
      loadGroups();
    } catch (err) {
      setGroupsError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка"));
    } finally {
      setLeavingGroupId(null);
    }
  }

  async function onDelete() {
    setDeleting(true);
    setDeleteError(null);
    try {
      await apiFetch(`/children/${id}`, { method: "DELETE" });
      setDeleted(true);
    } catch (err) {
      setDeleteError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка"));
      setDeleting(false);
    }
  }

  if (deleted) {
    return (
      <>
        <AppHeader />
        <main className="flex flex-1 flex-col items-center justify-center gap-4 p-8">
          <p className="text-ink-muted">Профіль дитини видалено.</p>
          <Link href="/dashboard" className="no-underline">
            <Button type="button" className="w-auto px-5">
              До кабінету
            </Button>
          </Link>
        </main>
      </>
    );
  }

  if (childLoading) {
    return (
      <>
        <AppHeader />
        <main className="flex flex-1 items-center justify-center p-8">
          <p className="text-ink-muted">Завантаження…</p>
        </main>
      </>
    );
  }

  if (childError || !child) {
    return (
      <>
        <AppHeader />
        <main className="flex flex-1 flex-col items-center justify-center gap-3 p-8">
          <p className="text-danger">
            {childError?.status === 404 ? "Дитину не знайдено." : (childError?.message ?? "Дитину не знайдено.")}
          </p>
          <Link href="/dashboard" className="text-sm">
            ← До кабінету
          </Link>
        </main>
      </>
    );
  }

  const status = STATUS[child.status];

  return (
    <>
      <AppHeader />

      <main className="flex flex-1 justify-center px-6 pt-7 pb-24">
        <div className="flex w-full max-w-220 flex-col gap-6">
          <Link href="/dashboard" className="text-ink-muted text-sm no-underline hover:underline">
            ← Мої діти
          </Link>

          <div className="flex flex-wrap items-center gap-4">
            <Avatar name={child.displayName} size={52} tint={tintFor(child.id)} />
            <div className="flex flex-col gap-[7px]">
              <h1 className="text-[30px] leading-[1.15] font-medium tracking-[-0.02em]">
                {child.displayName}
              </h1>
              <div className="flex flex-wrap items-center gap-2.5">
                <Pill tone={status.tone}>{status.label}</Pill>
                <span className="text-ink-icon text-sm">{child.birthYear} рік народження</span>
              </div>
            </div>
          </div>

          {child.status === "pending_consent" ? (
            <section className="bg-notice-surface border-notice-line flex flex-col gap-4 rounded-xl border px-6 py-5.5">
              <div className="flex items-start gap-3">
                <span
                  aria-hidden="true"
                  className="border-dev-mark text-notice-body mt-0.5 flex size-5.5 flex-none items-center justify-center rounded-full border text-[13px] font-semibold"
                >
                  !
                </span>
                <div className="flex flex-col gap-1.5">
                  <h2 className="text-notice-title text-lg font-medium">Потрібна згода батьків</h2>
                  <p className="text-notice-body text-sm leading-relaxed text-pretty">
                    Згідно з COPPA та GDPR ми обробляємо дані дитини лише за підтвердженою згодою
                    батьків або опікуна. До надання згоди {child.displayName} може проходити лише
                    демо-урок, а результати не зберігаються. Згоду можна відкликати будь-коли у
                    налаштуваннях профілю.
                  </p>
                </div>
              </div>
              <div className="flex flex-wrap items-center gap-3">
                <Button
                  type="button"
                  onClick={onConsent}
                  disabled={consenting}
                  className="h-11 w-auto px-5 text-[15px]"
                >
                  {consenting ? "Зберігаємо…" : "Надати згоду"}
                </Button>
                <Link href="/privacy" className="text-notice-title text-sm">
                  Що саме ми збираємо?
                </Link>
              </div>
            </section>
          ) : (
            <SecondaryLink href={`/play/${child.id}`}>Кабінет дитини → грати</SecondaryLink>
          )}

          <Section>
            <SectionHeader title="Групи" description="Клас або гурток, у якому дитина проходить уроки." />

            {groupsError ? (
              <p className="text-danger border-line-soft border-b px-6 py-4 text-sm">{groupsError.message}</p>
            ) : null}
            {!groups && !groupsError ? (
              <p className="text-ink-muted border-line-soft border-b px-6 py-4 text-sm">Завантаження…</p>
            ) : null}
            {groups && groups.length === 0 ? (
              <p className="text-ink-muted border-line-soft border-b px-6 py-4 text-sm">
                Дитина ще не в жодній групі.
              </p>
            ) : null}

            {groups?.map((group) => (
              <div
                key={group.id}
                className="border-line-soft flex items-center justify-between gap-4 border-b px-6 py-4"
              >
                <div className="flex min-w-0 flex-col gap-0.75">
                  <span className="font-medium">{group.name}</span>
                  <span className="text-ink-icon font-mono text-sm">
                    Код: {group.joinCode}
                    {!group.isActive ? " · архівна" : ""}
                  </span>
                </div>
                <QuietButton
                  type="button"
                  onClick={() => onLeaveGroup(group.id)}
                  disabled={leavingGroupId === group.id}
                >
                  {leavingGroupId === group.id ? "Виходимо…" : "Вийти з групи"}
                </QuietButton>
              </div>
            ))}

            {child.status === "active" ? (
              <form onSubmit={onJoinGroup} className="flex flex-col gap-2 px-6 pt-5 pb-6">
                <label htmlFor="join-code" className="text-[15px] font-medium">
                  Приєднатися за кодом
                </label>
                <div className="flex flex-wrap gap-2.5">
                  <TextInput
                    id="join-code"
                    type="text"
                    required
                    value={joinCode}
                    onChange={(e) => setJoinCode(e.target.value.toUpperCase())}
                    placeholder="Напр. 4F7-2KD"
                    className="h-11 min-w-50 flex-1 font-mono tracking-[0.06em]"
                  />
                  <SecondaryButton type="submit" disabled={joining} className="h-11 px-5 text-[15px]">
                    {joining ? "Приєднуємо…" : "Приєднатися"}
                  </SecondaryButton>
                </div>
                {joinError ? (
                  <p className="text-danger text-[13px] leading-snug">{joinErrorText(joinError)}</p>
                ) : (
                  <p className="text-ink-soft text-[13px] leading-snug">
                    Код видає вчитель. Одна дитина може бути щонайбільше у трьох групах.
                  </p>
                )}
              </form>
            ) : (
              <p className="text-ink-soft px-6 pt-5 pb-6 text-sm">
                Спершу надайте згоду вище, щоб приєднатися до групи.
              </p>
            )}
          </Section>

          <Section>
            <SectionHeader
              title="Час гри"
              description="Скільки дитина грає за один підхід. Ліміт ніколи не перериває урок: після уроку, на якому час вичерпано, провідник прощається й «засинає», а дитина відпочиває. Таймера дитина не бачить."
            />
            <div className="flex flex-col gap-4 px-6 pt-1 pb-6">
              {!playState && !playError ? <p className="text-ink-muted text-sm">Завантаження…</p> : null}
              {playState ? (
                <div role="radiogroup" aria-label="Ліміт часу гри" className="flex flex-wrap gap-2">
                  {SESSION_LIMITS.map((option) => {
                    const selected = playState.sessionLimitMinutes === option.value;
                    return (
                      <button
                        key={option.label}
                        type="button"
                        role="radio"
                        aria-checked={selected}
                        disabled={savingLimit}
                        onClick={() => onSetLimit(option.value)}
                        className={`h-10 cursor-pointer rounded-lg border px-4 text-[15px] font-medium transition-colors duration-100 disabled:cursor-default ${
                          selected
                            ? "bg-brand border-brand text-white"
                            : "bg-surface border-line-field text-ink hover:bg-row-hover"
                        }`}
                      >
                        {option.label}
                      </button>
                    );
                  })}
                </div>
              ) : null}
              {playState?.resting && playState.restUntil ? (
                <div className="bg-notice-surface border-notice-line flex flex-wrap items-center justify-between gap-3 rounded-lg border px-4 py-3">
                  <span className="text-notice-title text-[15px] font-medium">
                    Відпочиває до{" "}
                    {new Date(playState.restUntil).toLocaleTimeString("uk-UA", { hour: "2-digit", minute: "2-digit" })}
                  </span>
                  <SecondaryButton type="button" onClick={onWake} disabled={waking} className="h-10 px-4 text-[15px]">
                    {waking ? "Будимо…" : "Дозволити грати зараз"}
                  </SecondaryButton>
                </div>
              ) : null}
              {playError ? <p className="text-danger text-sm">{playError.message}</p> : null}
            </div>
          </Section>

          {child.status === "active" ? (
            <Section>
              <SectionHeader
                title="Оформлення"
                description="Дитина може сама обрати улюблений колір, «світ» (космос, море, ліс…) і настрій — і дитячий режим перефарбується під її смак. Ми зберігаємо лише ці три вибори зі списку, без жодного тексту. Нові теми спершу перевіряє людина. Відкликання згоди одразу видаляє вибір дитини."
              />
              <div className="flex flex-col gap-4 px-6 pt-1 pb-6">
                {!style && !styleError ? <p className="text-ink-muted text-sm">Завантаження…</p> : null}
                {style ? (
                  <label className="flex cursor-pointer items-start gap-3">
                    <input
                      type="checkbox"
                      className="accent-brand mt-1 size-4.5 cursor-pointer"
                      checked={style.consented}
                      disabled={styleSaving}
                      onChange={(e) => (e.target.checked ? void onStyleConsent(true) : setRevokeOpen(true))}
                    />
                    <span className="flex flex-col gap-0.5">
                      <span className="text-[15px] font-medium">Дозволити дитині обирати оформлення</span>
                      <span className="text-ink-soft text-[13px]">{consentHistoryText(styleConsent)}</span>
                    </span>
                  </label>
                ) : null}
                {style?.consented && style.answers ? (
                  <div className="border-line-soft flex flex-wrap items-center gap-x-6 gap-y-3 rounded-lg border px-4 py-3">
                    <span className="flex items-center gap-2 text-[15px]">
                      <span
                        aria-hidden="true"
                        className="size-5 rounded-full border border-black/10"
                        style={{ background: COLOR_SPOTS[style.answers.colorId] }}
                      />
                      {COLOR_LABELS[style.answers.colorId]}
                    </span>
                    <span className="text-[15px]">Світ: {WORLD_LABELS[style.answers.worldId]}</span>
                    <span className="text-[15px]">Настрій: {MOOD_LABELS[style.answers.moodId]}</span>
                    <Pill tone={style.pending ? "waiting" : "ok"}>{style.pending ? "Готується" : "Застосовано"}</Pill>
                  </div>
                ) : null}
                {style?.consented && !style.answers ? (
                  <p className="text-ink-muted text-sm">Дитина ще нічого не обрала — кнопка-пензлик на її полиці уроків.</p>
                ) : null}
                {styleError ? <p className="text-danger text-sm">{styleError.message}</p> : null}
              </div>
            </Section>
          ) : null}

          <Section>
            <SectionHeader
              title="Історія уроків"
              description="Натисніть на спробу, щоб побачити відповіді по сценах."
              aside={history ? attemptsLabel(history.length) : undefined}
            />

            {historyError ? (
              <p className="text-danger px-6 py-4 text-sm">{historyError.message}</p>
            ) : null}
            {!history && !historyError ? (
              <p className="text-ink-muted px-6 py-4 text-sm">Завантаження…</p>
            ) : null}
            {history && history.length === 0 ? (
              <p className="text-ink-muted px-6 py-4 text-sm">Ще немає жодного проходження.</p>
            ) : null}

            {history?.map((entry) => {
              const expanded = expandedAttemptId === entry.attemptId;
              const result = entry.result ? RESULTS[entry.result] : null;
              return (
                <div key={entry.attemptId} className="border-line-soft border-b last:border-b-0">
                  <button
                    type="button"
                    onClick={() => setExpandedAttemptId(expanded ? null : entry.attemptId)}
                    aria-expanded={expanded}
                    className="hover:bg-row-hover flex w-full cursor-pointer items-center gap-3.5 px-6 py-4 text-left transition-colors duration-100"
                  >
                    <span
                      aria-hidden="true"
                      className={`border-ink-icon size-2.25 flex-none border-r-[1.6px] border-b-[1.6px] transition-transform duration-150 ${
                        expanded ? "rotate-[225deg]" : "-rotate-45"
                      }`}
                    />
                    <span className="flex min-w-0 flex-1 flex-col gap-0.75">
                      <span className="font-medium">{entry.title}</span>
                      <span className="text-ink-icon text-sm">
                        {new Date(entry.startedAt).toLocaleString("uk-UA")}
                      </span>
                    </span>
                    <Pill tone={result ? result.tone : "waiting"}>
                      {result ? result.label : "У процесі"}
                    </Pill>
                    <span className="text-avatar-ink w-15.5 flex-none text-right font-mono text-[15px]">
                      {entry.score != null ? `${Math.round(entry.score * 100)}%` : "—"}
                    </span>
                  </button>

                  {expanded ? (
                    <div className="bg-row-hover flex flex-col gap-2.5 pt-1 pr-6 pb-5 pl-[47px]">
                      <div className="text-ink-faint border-line-head grid grid-cols-[1fr_1fr_auto] gap-3 border-b pb-1.5 font-mono text-xs tracking-[0.08em] uppercase">
                        <span>Сцена</span>
                        <span>Відповідь дитини</span>
                        <span>Підказки</span>
                      </div>
                      {entry.answers.map((answer, i) => (
                        <div
                          key={`${answer.sceneKey}-${answer.tryNo}-${i}`}
                          className="border-line-row grid grid-cols-[1fr_1fr_auto] items-start gap-3 border-b pb-2.5 text-sm leading-relaxed"
                        >
                          <span className="text-avatar-ink">
                            {answer.sceneKey}
                            {answer.tryNo > 1 ? ` · спроба ${answer.tryNo}` : ""}
                          </span>
                          <span className="flex items-start gap-2">
                            <span
                              aria-hidden="true"
                              className={`mt-0.5 flex size-4 flex-none items-center justify-center rounded-full text-[10px] font-semibold text-white ${
                                answer.isCorrect ? "bg-mark-ok" : "bg-danger"
                              }`}
                            >
                              {answer.isCorrect ? "✓" : "✕"}
                            </span>
                            <span>Варіант {answer.chosenOption}</span>
                          </span>
                          <span className="text-ink-icon text-right font-mono">{answer.hintsUsed}</span>
                        </div>
                      ))}
                    </div>
                  ) : null}
                </div>
              );
            })}
          </Section>

          <section className="bg-surface border-danger-line-soft flex flex-col gap-4 rounded-xl border px-6 py-5.5">
            <div className="flex flex-col gap-1.5">
              <h2 className="text-danger-title text-lg font-medium">Небезпечна зона</h2>
              <p className="text-ink-soft text-sm leading-relaxed text-pretty">
                Видалення профілю остаточно стирає всю історію уроків, відповіді та прогрес
                {" "}
                {child.displayName}. Дані не можна відновити, і дитина втратить доступ до груп.
              </p>
            </div>
            <div className="flex flex-wrap items-center gap-3">
              <DangerButton type="button" onClick={() => setConfirmOpen(true)}>
                Видалити дитину
              </DangerButton>
            </div>
          </section>
        </div>
      </main>

      {revokeOpen ? (
        <Modal
          title="Відкликати згоду на оформлення?"
          onClose={() => setRevokeOpen(false)}
          footer={
            <>
              <QuietButton type="button" className="h-11 px-4 text-[15px]" onClick={() => setRevokeOpen(false)}>
                Скасувати
              </QuietButton>
              <DangerButton type="button" onClick={() => void onStyleConsent(false)} disabled={styleSaving}>
                {styleSaving ? "Відкликаємо…" : "Відкликати"}
              </DangerButton>
            </>
          }
        >
          <p className="text-ink-soft text-sm leading-relaxed text-pretty">
            Вибір дитини буде видалено, а дитячий режим повернеться до стандартного оформлення.
          </p>
        </Modal>
      ) : null}

      {confirmOpen ? (
        <Modal
          title="Підтвердьте видалення"
          onClose={() => {
            setConfirmOpen(false);
            setConfirmText("");
            setDeleteError(null);
          }}
          footer={
            <>
              <QuietButton
                type="button"
                className="h-11 px-4 text-[15px]"
                onClick={() => {
                  setConfirmOpen(false);
                  setConfirmText("");
                  setDeleteError(null);
                }}
              >
                Скасувати
              </QuietButton>
              <DangerButton
                type="button"
                onClick={onDelete}
                disabled={confirmText !== child.displayName || deleting}
              >
                {deleting ? "Видаляємо…" : "Видалити назавжди"}
              </DangerButton>
            </>
          }
        >
          <p className="text-ink-soft text-sm leading-relaxed text-pretty">
            Щоб назавжди видалити <strong className="text-ink font-medium">{child.displayName}</strong>{" "}
            та всю історію уроків, введіть ім&apos;я дитини нижче.
          </p>
          <TextInput
            type="text"
            value={confirmText}
            onChange={(e) => setConfirmText(e.target.value)}
            placeholder={child.displayName}
            aria-label="Ім'я дитини для підтвердження"
          />
          {deleteError ? <p className="text-danger text-sm">{deleteError.message}</p> : null}
        </Modal>
      ) : null}
    </>
  );
}

function consentHistoryText(consent: ConsentEntry | null): string {
  if (!consent) return "Згоду ще не надавали.";
  const date = (iso: string) => new Date(iso).toLocaleString("uk-UA", { dateStyle: "medium", timeStyle: "short" });
  return consent.revokedAt
    ? `Згоду надано ${date(consent.grantedAt)}, відкликано ${date(consent.revokedAt)}.`
    : `Згоду надано ${date(consent.grantedAt)}.`;
}

function joinErrorText(error: ApiError): string {
  if (error.code === "CHILD_NOT_ACTIVE") return "Спершу надайте згоду для цієї дитини.";
  if (error.code === "GROUP_INACTIVE") return "Ця група більше не активна.";
  return error.message;
}

function attemptsLabel(n: number): string {
  const mod100 = n % 100;
  const mod10 = n % 10;
  if (mod100 >= 11 && mod100 <= 14) return `${n} спроб`;
  if (mod10 === 1) return `${n} спроба`;
  if (mod10 >= 2 && mod10 <= 4) return `${n} спроби`;
  return `${n} спроб`;
}
