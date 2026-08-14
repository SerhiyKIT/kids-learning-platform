"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { apiFetch, ApiError } from "@/lib/api";
import type { Child, Group, HistoryEntry } from "@/lib/api-types";

const STATUS_BADGES: Record<Child["status"], { label: string; className: string }> = {
  pending_consent: { label: "Очікує згоди", className: "bg-amber-100 text-amber-800" },
  active: { label: "Активна", className: "bg-emerald-100 text-emerald-800" },
};

const RESULT_LABELS: Record<string, string> = {
  completed: "Завершено",
  abandoned: "Перервано",
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
      .catch((err) => setChildError(err instanceof ApiError ? err : new ApiError(0, "Unexpected error")))
      .finally(() => setChildLoading(false));
  }, [id]);

  const loadGroups = useCallback(() => {
    apiFetch<Group[]>(`/children/${id}/groups`)
      .then(setGroups)
      .catch((err) => setGroupsError(err instanceof ApiError ? err : new ApiError(0, "Unexpected error")));
  }, [id]);

  const loadHistory = useCallback(() => {
    apiFetch<HistoryEntry[]>(`/children/${id}/history`)
      .then(setHistory)
      .catch((err) => setHistoryError(err instanceof ApiError ? err : new ApiError(0, "Unexpected error")));
  }, [id]);

  useEffect(() => {
    loadChild();
    loadGroups();
    loadHistory();
  }, [loadChild, loadGroups, loadHistory]);

  async function onConsent() {
    setConsenting(true);
    try {
      await apiFetch(`/children/${id}/consent`, { method: "POST" });
      loadChild();
    } catch (err) {
      setChildError(err instanceof ApiError ? err : new ApiError(0, "Unexpected error"));
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
      setJoinError(err instanceof ApiError ? err : new ApiError(0, "Unexpected error"));
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
      setGroupsError(err instanceof ApiError ? err : new ApiError(0, "Unexpected error"));
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
      setDeleteError(err instanceof ApiError ? err : new ApiError(0, "Unexpected error"));
      setDeleting(false);
    }
  }

  if (deleted) {
    return (
      <main className="mx-auto flex max-w-sm flex-1 flex-col justify-center gap-4 p-8">
        <p>Дитину видалено.</p>
        <Link href="/dashboard" className="rounded-full bg-foreground px-5 py-2 text-background text-center">
          До кабінету
        </Link>
      </main>
    );
  }

  if (childLoading) {
    return (
      <main className="flex flex-1 items-center justify-center p-8">
        <p>Завантаження…</p>
      </main>
    );
  }

  if (childError || !child) {
    return (
      <main className="mx-auto flex max-w-sm flex-1 flex-col justify-center gap-4 p-8">
        <p className="text-red-600">
          {childError?.status === 404 ? "Дитину не знайдено." : childError?.message ?? "Дитину не знайдено."}
        </p>
        <Link href="/dashboard" className="underline">
          ← До кабінету
        </Link>
      </main>
    );
  }

  const badge = STATUS_BADGES[child.status];

  return (
    <main className="mx-auto flex w-full max-w-2xl flex-1 flex-col gap-6 p-6 sm:p-8">
      <div>
        <Link href="/dashboard" className="text-sm text-blue-600 underline">
          ← До кабінету
        </Link>
        <div className="mt-2 flex items-center gap-3">
          <span className="text-4xl">🧒</span>
          <div>
            <h1 className="text-2xl font-semibold">{child.displayName}</h1>
            <span className={`rounded-full px-2 py-0.5 text-xs ${badge.className}`}>{badge.label}</span>
          </div>
        </div>
      </div>

      {child.status === "pending_consent" ? (
        <div className="flex flex-col gap-2 rounded-2xl border border-amber-300 bg-amber-50 p-4 text-amber-900">
          <p className="font-medium">Потрібна згода на обробку даних дитини</p>
          <p className="text-sm">
            Перш ніж дитина зможе грати уроки або приєднатися до групи, підтвердьте, що ви даєте
            згоду на створення облікового запису дитини та обробку її даних (COPPA/GDPR).
          </p>
          <button
            type="button"
            onClick={onConsent}
            disabled={consenting}
            className="self-start rounded-full bg-amber-600 px-4 py-2 text-white disabled:opacity-60"
          >
            {consenting ? "Зберігаємо…" : "Надати згоду"}
          </button>
        </div>
      ) : null}

      <Link
        href={`/play/${child.id}`}
        className="self-start rounded-full bg-blue-600 px-5 py-2 text-white"
      >
        Грати
      </Link>

      <section className="flex flex-col gap-3">
        <h2 className="text-lg font-semibold">Групи</h2>
        {groupsError ? <p className="text-red-600">{groupsError.message}</p> : null}
        {!groups && !groupsError ? <p>Завантаження…</p> : null}
        {groups && groups.length === 0 ? <p className="text-black/60 dark:text-white/60">Дитина ще не в жодній групі.</p> : null}
        {groups && groups.length > 0 ? (
          <ul className="flex flex-col gap-2">
            {groups.map((group) => (
              <li
                key={group.id}
                className="flex items-center justify-between rounded-xl border border-black/10 p-3 dark:border-white/10"
              >
                <span>
                  {group.name}
                  {!group.isActive ? <span className="ml-2 text-xs text-black/50">(архівна)</span> : null}
                </span>
                <button
                  type="button"
                  onClick={() => onLeaveGroup(group.id)}
                  disabled={leavingGroupId === group.id}
                  className="rounded-full border border-black/[.16] px-3 py-1 text-sm disabled:opacity-60 dark:border-white/[.2]"
                >
                  {leavingGroupId === group.id ? "Виходимо…" : "Вийти з групи"}
                </button>
              </li>
            ))}
          </ul>
        ) : null}

        {child.status === "active" ? (
          <form onSubmit={onJoinGroup} className="flex items-end gap-2">
            <label className="flex flex-1 flex-col gap-1">
              <span className="text-sm">Приєднатися за кодом</span>
              <input
                type="text"
                required
                value={joinCode}
                onChange={(e) => setJoinCode(e.target.value)}
                placeholder="Код групи"
                className="rounded border border-black/[.16] px-3 py-2 uppercase dark:border-white/[.2]"
              />
            </label>
            <button
              type="submit"
              disabled={joining}
              className="rounded-full bg-foreground px-4 py-2 text-background disabled:opacity-50"
            >
              {joining ? "Приєднуємо…" : "Приєднатися"}
            </button>
          </form>
        ) : (
          <p className="text-sm text-black/60 dark:text-white/60">
            Спершу надайте згоду вище, щоб приєднатися до групи.
          </p>
        )}
        {joinError ? <JoinErrorMessage error={joinError} /> : null}
      </section>

      <section className="flex flex-col gap-3">
        <h2 className="text-lg font-semibold">Історія проходжень</h2>
        {historyError ? <p className="text-red-600">{historyError.message}</p> : null}
        {!history && !historyError ? <p>Завантаження…</p> : null}
        {history && history.length === 0 ? (
          <p className="text-black/60 dark:text-white/60">Ще немає проходжень.</p>
        ) : null}
        {history && history.length > 0 ? (
          <ul className="flex flex-col gap-2">
            {history.map((entry) => {
              const expanded = expandedAttemptId === entry.attemptId;
              return (
                <li key={entry.attemptId} className="rounded-xl border border-black/10 dark:border-white/10">
                  <button
                    type="button"
                    onClick={() => setExpandedAttemptId(expanded ? null : entry.attemptId)}
                    className="flex w-full items-center justify-between gap-3 p-3 text-left"
                  >
                    <span className="flex flex-col">
                      <span className="font-medium">{entry.title}</span>
                      <span className="text-sm text-black/60 dark:text-white/60">
                        {new Date(entry.startedAt).toLocaleString("uk-UA")}
                        {entry.result ? ` — ${RESULT_LABELS[entry.result] ?? entry.result}` : " — у процесі"}
                        {entry.score != null ? ` — ${Math.round(entry.score * 100)}%` : ""}
                      </span>
                    </span>
                    <span className="text-sm text-blue-600">{expanded ? "Згорнути" : "Деталі"}</span>
                  </button>
                  {expanded ? (
                    <ul className="flex flex-col gap-1 border-t border-black/10 p-3 text-sm dark:border-white/10">
                      {entry.answers.map((answer, i) => (
                        <li key={i} className="flex justify-between gap-2">
                          <span>
                            {answer.sceneKey} (спроба {answer.tryNo}, варіант {answer.chosenOption})
                          </span>
                          <span className={answer.isCorrect ? "text-emerald-700" : "text-red-600"}>
                            {answer.isCorrect ? "правильно" : "неправильно"}
                            {answer.hintsUsed > 0 ? ` · підказок: ${answer.hintsUsed}` : ""}
                          </span>
                        </li>
                      ))}
                    </ul>
                  ) : null}
                </li>
              );
            })}
          </ul>
        ) : null}
      </section>

      <section className="flex flex-col gap-3 rounded-2xl border border-red-300 bg-red-50 p-4 dark:border-red-900/50 dark:bg-red-950/30">
        <h2 className="text-lg font-semibold text-red-800 dark:text-red-300">Небезпечна зона</h2>
        <p className="text-sm text-red-800 dark:text-red-300">
          Видалення дитини незворотно стирає весь її профіль, участь у групах та історію
          проходжень уроків. Це неможливо скасувати.
        </p>
        <button
          type="button"
          onClick={() => setConfirmOpen(true)}
          className="self-start rounded-full bg-red-700 px-4 py-2 text-white"
        >
          Видалити дитину
        </button>
      </section>

      {confirmOpen ? (
        <div className="fixed inset-0 flex items-center justify-center bg-black/50 p-4">
          <div className="flex w-full max-w-sm flex-col gap-3 rounded-2xl bg-white p-6 dark:bg-neutral-900">
            <h3 className="text-lg font-semibold">Підтвердьте видалення</h3>
            <p className="text-sm">
              Щоб видалити <strong>{child.displayName}</strong> та всю історію назавжди, введіть
              ім&apos;я дитини нижче.
            </p>
            <input
              type="text"
              value={confirmText}
              onChange={(e) => setConfirmText(e.target.value)}
              placeholder={child.displayName}
              className="rounded border border-black/[.16] px-3 py-2 dark:border-white/[.2]"
            />
            {deleteError ? <p className="text-red-600">{deleteError.message}</p> : null}
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => {
                  setConfirmOpen(false);
                  setConfirmText("");
                  setDeleteError(null);
                }}
                className="rounded-full border border-black/[.16] px-4 py-2 dark:border-white/[.2]"
              >
                Скасувати
              </button>
              <button
                type="button"
                onClick={onDelete}
                disabled={confirmText !== child.displayName || deleting}
                className="rounded-full bg-red-700 px-4 py-2 text-white disabled:opacity-50"
              >
                {deleting ? "Видаляємо…" : "Видалити назавжди"}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </main>
  );
}

function JoinErrorMessage({ error }: { error: ApiError }) {
  if (error.code === "CHILD_NOT_ACTIVE") {
    return <p className="text-red-600">Спершу надайте згоду для цієї дитини.</p>;
  }
  if (error.code === "GROUP_INACTIVE") {
    return <p className="text-red-600">Ця група більше не активна.</p>;
  }
  return <p className="text-red-600">{error.message}</p>;
}
