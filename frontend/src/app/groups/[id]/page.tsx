"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { apiFetch, ApiError } from "@/lib/api";
import type {
  Assignment,
  CatalogEntry,
  CreateAssignmentRequest,
  Group,
  GroupMemberInfo,
  TeacherResultChild,
} from "@/lib/api-types";

const RESULT_LABELS: Record<string, string> = {
  completed: "Завершено",
  abandoned: "Перервано",
};

export default function GroupPage() {
  const { id } = useParams<{ id: string }>();

  const [group, setGroup] = useState<Group | null>(null);
  const [groupError, setGroupError] = useState<ApiError | null>(null);
  const [groupLoading, setGroupLoading] = useState(true);
  const [regenerating, setRegenerating] = useState(false);
  const [copied, setCopied] = useState(false);

  const [members, setMembers] = useState<GroupMemberInfo[] | null>(null);
  const [membersError, setMembersError] = useState<ApiError | null>(null);
  const [removingChildId, setRemovingChildId] = useState<string | null>(null);

  const [catalog, setCatalog] = useState<CatalogEntry[] | null>(null);
  const [catalogError, setCatalogError] = useState<ApiError | null>(null);
  const [assigning, setAssigning] = useState(false);
  const [selectedVersionId, setSelectedVersionId] = useState("");
  const [assignError, setAssignError] = useState<ApiError | null>(null);

  const [assignments, setAssignments] = useState<Assignment[] | null>(null);
  const [assignmentsError, setAssignmentsError] = useState<ApiError | null>(null);
  const [removingAssignmentId, setRemovingAssignmentId] = useState<string | null>(null);

  const [results, setResults] = useState<TeacherResultChild[] | null>(null);
  const [resultsError, setResultsError] = useState<ApiError | null>(null);

  const [confirmArchiveOpen, setConfirmArchiveOpen] = useState(false);
  const [archiving, setArchiving] = useState(false);
  const [archiveError, setArchiveError] = useState<ApiError | null>(null);

  // No GET /api/groups/{id} exists — the teacher's own list is the only source for a single
  // group's current name/joinCode/isActive, so this loads the whole list and picks this one out.
  const loadGroup = useCallback(() => {
    apiFetch<Group[]>("/groups")
      .then((allGroups) => {
        const found = allGroups.find((g) => g.id === id);
        if (!found) {
          setGroupError(new ApiError(404, "Групу не знайдено"));
          return;
        }
        setGroup(found);
        setGroupError(null);
      })
      .catch((err) => setGroupError(err instanceof ApiError ? err : new ApiError(0, "Unexpected error")))
      .finally(() => setGroupLoading(false));
  }, [id]);

  const loadMembers = useCallback(() => {
    apiFetch<GroupMemberInfo[]>(`/groups/${id}/members`)
      .then(setMembers)
      .catch((err) => setMembersError(err instanceof ApiError ? err : new ApiError(0, "Unexpected error")));
  }, [id]);

  const loadAssignments = useCallback(() => {
    apiFetch<Assignment[]>(`/assignments?groupId=${id}`)
      .then(setAssignments)
      .catch((err) => setAssignmentsError(err instanceof ApiError ? err : new ApiError(0, "Unexpected error")));
  }, [id]);

  const loadResults = useCallback(() => {
    apiFetch<TeacherResultChild[]>(`/groups/${id}/results`)
      .then(setResults)
      .catch((err) => setResultsError(err instanceof ApiError ? err : new ApiError(0, "Unexpected error")));
  }, [id]);

  const loadCatalog = useCallback(() => {
    apiFetch<CatalogEntry[]>("/catalog/lessons")
      .then(setCatalog)
      .catch((err) => setCatalogError(err instanceof ApiError ? err : new ApiError(0, "Unexpected error")));
  }, []);

  useEffect(() => {
    loadGroup();
    loadMembers();
    loadAssignments();
    loadResults();
    loadCatalog();
  }, [loadGroup, loadMembers, loadAssignments, loadResults, loadCatalog]);

  async function onCopy() {
    if (!group) return;
    try {
      await navigator.clipboard.writeText(group.joinCode);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Nothing more useful to do — the code is already visible on the page.
    }
  }

  async function onRegenerateCode() {
    setRegenerating(true);
    try {
      const updated = await apiFetch<Group>(`/groups/${id}/regenerate-code`, { method: "POST" });
      setGroup(updated);
    } catch (err) {
      setGroupError(err instanceof ApiError ? err : new ApiError(0, "Unexpected error"));
    } finally {
      setRegenerating(false);
    }
  }

  async function onRemoveMember(childId: string) {
    setRemovingChildId(childId);
    try {
      await apiFetch(`/groups/${id}/members/${childId}`, { method: "DELETE" });
      loadMembers();
    } catch (err) {
      setMembersError(err instanceof ApiError ? err : new ApiError(0, "Unexpected error"));
    } finally {
      setRemovingChildId(null);
    }
  }

  async function onAssign(e: React.FormEvent) {
    e.preventDefault();
    if (!selectedVersionId) return;
    setAssigning(true);
    setAssignError(null);
    try {
      const body: CreateAssignmentRequest = { lessonVersionId: selectedVersionId, groupId: id };
      await apiFetch<Assignment>("/assignments", { method: "POST", body: JSON.stringify(body) });
      setSelectedVersionId("");
      loadAssignments();
    } catch (err) {
      setAssignError(err instanceof ApiError ? err : new ApiError(0, "Unexpected error"));
    } finally {
      setAssigning(false);
    }
  }

  async function onRemoveAssignment(assignmentId: string) {
    setRemovingAssignmentId(assignmentId);
    try {
      await apiFetch(`/assignments/${assignmentId}`, { method: "DELETE" });
      loadAssignments();
    } catch (err) {
      setAssignmentsError(err instanceof ApiError ? err : new ApiError(0, "Unexpected error"));
    } finally {
      setRemovingAssignmentId(null);
    }
  }

  async function onArchive() {
    setArchiving(true);
    setArchiveError(null);
    try {
      await apiFetch(`/groups/${id}/archive`, { method: "POST" });
      setConfirmArchiveOpen(false);
      loadGroup();
    } catch (err) {
      setArchiveError(err instanceof ApiError ? err : new ApiError(0, "Unexpected error"));
    } finally {
      setArchiving(false);
    }
  }

  function catalogTitleFor(lessonVersionId: string): string {
    const entry = catalog?.find((c) => c.currentVersionId === lessonVersionId);
    return entry ? `${entry.title} (${entry.moduleCode})` : lessonVersionId;
  }

  if (groupLoading) {
    return (
      <main className="flex flex-1 items-center justify-center p-8">
        <p>Завантаження…</p>
      </main>
    );
  }

  if (groupError || !group) {
    return (
      <main className="mx-auto flex max-w-sm flex-1 flex-col justify-center gap-4 p-8">
        <p className="text-red-600">
          {groupError?.status === 404 ? "Групу не знайдено." : groupError?.message ?? "Групу не знайдено."}
        </p>
        <Link href="/groups" className="underline">
          ← До груп
        </Link>
      </main>
    );
  }

  const assignedVersionIds = new Set(assignments?.map((a) => a.lessonVersionId) ?? []);
  const availableCatalog = catalog?.filter((c) => !assignedVersionIds.has(c.currentVersionId)) ?? [];

  return (
    <main className="mx-auto flex w-full max-w-2xl flex-1 flex-col gap-6 p-6 sm:p-8">
      <div>
        <Link href="/groups" className="text-sm text-blue-600 underline">
          ← До груп
        </Link>
        <div className="mt-2 flex items-center gap-3">
          <span className="text-4xl">👥</span>
          <div>
            <h1 className="text-2xl font-semibold">{group.name}</h1>
            <span
              className={`rounded-full px-2 py-0.5 text-xs ${
                group.isActive
                  ? "bg-emerald-100 text-emerald-800"
                  : "bg-black/10 text-black/60 dark:bg-white/10 dark:text-white/60"
              }`}
            >
              {group.isActive ? "Активна" : "Архів"}
            </span>
          </div>
        </div>
      </div>

      {group.isActive ? (
        <div className="flex flex-col gap-2 rounded-2xl border border-black/10 p-4 dark:border-white/10">
          <p className="text-sm text-black/60 dark:text-white/60">Код для приєднання</p>
          <div className="flex flex-wrap items-center gap-3">
            <span className="rounded-lg bg-black/5 px-3 py-1.5 font-mono text-xl tracking-wider dark:bg-white/10">
              {group.joinCode}
            </span>
            <button
              type="button"
              onClick={onCopy}
              className="rounded-full border border-black/[.16] px-3 py-1.5 text-sm dark:border-white/[.2]"
            >
              {copied ? "Скопійовано" : "Копіювати"}
            </button>
            <button
              type="button"
              onClick={onRegenerateCode}
              disabled={regenerating}
              className="rounded-full border border-black/[.16] px-3 py-1.5 text-sm disabled:opacity-60 dark:border-white/[.2]"
            >
              {regenerating ? "Оновлюємо…" : "Згенерувати новий код"}
            </button>
          </div>
        </div>
      ) : (
        <p className="rounded-2xl border border-black/10 p-4 text-sm text-black/60 dark:border-white/10 dark:text-white/60">
          Ця група архівна — нові діти не можуть приєднатися за кодом.
        </p>
      )}

      <section className="flex flex-col gap-3">
        <h2 className="text-lg font-semibold">Учасники</h2>
        {membersError ? <p className="text-red-600">{membersError.message}</p> : null}
        {!members && !membersError ? <p>Завантаження…</p> : null}
        {members && members.length === 0 ? (
          <p className="text-black/60 dark:text-white/60">
            Ще ніхто не приєднався. Поділіться кодом {group.joinCode}.
          </p>
        ) : null}
        {members && members.length > 0 ? (
          <ul className="flex flex-col gap-2">
            {members.map((member) => (
              <li
                key={member.childId}
                className="flex items-center justify-between rounded-xl border border-black/10 p-3 dark:border-white/10"
              >
                <span className="flex items-center gap-2">
                  <span className="text-xl">🧒</span>
                  {member.displayName}
                </span>
                <button
                  type="button"
                  onClick={() => onRemoveMember(member.childId)}
                  disabled={removingChildId === member.childId}
                  className="rounded-full border border-black/[.16] px-3 py-1 text-sm disabled:opacity-60 dark:border-white/[.2]"
                >
                  {removingChildId === member.childId ? "Вилучаємо…" : "Вилучити"}
                </button>
              </li>
            ))}
          </ul>
        ) : null}
      </section>

      <section className="flex flex-col gap-3">
        <h2 className="text-lg font-semibold">Призначені уроки</h2>
        {assignmentsError ? <p className="text-red-600">{assignmentsError.message}</p> : null}
        {!assignments && !assignmentsError ? <p>Завантаження…</p> : null}
        {assignments && assignments.length === 0 ? (
          <p className="text-black/60 dark:text-white/60">Ще немає призначених уроків.</p>
        ) : null}
        {assignments && assignments.length > 0 ? (
          <ul className="flex flex-col gap-2">
            {assignments.map((assignment) => (
              <li
                key={assignment.id}
                className="flex items-center justify-between rounded-xl border border-black/10 p-3 dark:border-white/10"
              >
                <span>{catalog ? catalogTitleFor(assignment.lessonVersionId) : assignment.lessonVersionId}</span>
                <button
                  type="button"
                  onClick={() => onRemoveAssignment(assignment.id)}
                  disabled={removingAssignmentId === assignment.id}
                  className="rounded-full border border-black/[.16] px-3 py-1 text-sm disabled:opacity-60 dark:border-white/[.2]"
                >
                  {removingAssignmentId === assignment.id ? "Прибираємо…" : "Прибрати"}
                </button>
              </li>
            ))}
          </ul>
        ) : null}

        <form onSubmit={onAssign} className="flex items-end gap-2">
          <label className="flex flex-1 flex-col gap-1">
            <span className="text-sm">Призначити урок</span>
            <select
              required
              value={selectedVersionId}
              onChange={(e) => setSelectedVersionId(e.target.value)}
              className="rounded border border-black/[.16] px-3 py-2 dark:border-white/[.2]"
            >
              <option value="" disabled>
                Оберіть урок з каталогу
              </option>
              {availableCatalog.map((entry) => (
                <option key={entry.currentVersionId} value={entry.currentVersionId}>
                  {entry.title} ({entry.moduleCode})
                </option>
              ))}
            </select>
          </label>
          <button
            type="submit"
            disabled={assigning || !selectedVersionId}
            className="rounded-full bg-foreground px-4 py-2 text-background disabled:opacity-50"
          >
            {assigning ? "Призначаємо…" : "Призначити"}
          </button>
        </form>
        {catalogError ? <p className="text-red-600">{catalogError.message}</p> : null}
        {catalog && catalog.length === 0 ? (
          <p className="text-sm text-black/60 dark:text-white/60">У каталозі ще немає опублікованих уроків.</p>
        ) : null}
        {assignError ? <AssignErrorMessage error={assignError} /> : null}
      </section>

      <section className="flex flex-col gap-3">
        <h2 className="text-lg font-semibold">Результати</h2>
        {resultsError ? <p className="text-red-600">{resultsError.message}</p> : null}
        {!results && !resultsError ? <p>Завантаження…</p> : null}
        {results && results.length === 0 ? (
          <p className="text-black/60 dark:text-white/60">Ще немає результатів.</p>
        ) : null}
        {results && results.length > 0 ? (
          <ul className="flex flex-col gap-3">
            {results.map((child) => (
              <li key={child.childId} className="rounded-xl border border-black/10 p-3 dark:border-white/10">
                <p className="mb-2 font-medium">{child.displayName}</p>
                {child.attempts.length === 0 ? (
                  <p className="text-sm text-black/60 dark:text-white/60">Ще немає проходжень.</p>
                ) : (
                  <ul className="flex flex-col gap-1 text-sm">
                    {child.attempts.map((attempt, i) => (
                      <li key={i} className="flex flex-wrap items-center justify-between gap-2">
                        <span>{attempt.title}</span>
                        <span className="text-black/60 dark:text-white/60">
                          {attempt.completedAt ? new Date(attempt.completedAt).toLocaleDateString("uk-UA") : "у процесі"}
                          {attempt.result ? ` — ${RESULT_LABELS[attempt.result] ?? attempt.result}` : ""}
                          {attempt.score != null ? ` — ${Math.round(attempt.score * 100)}%` : ""}
                        </span>
                      </li>
                    ))}
                  </ul>
                )}
              </li>
            ))}
          </ul>
        ) : null}
      </section>

      {group.isActive ? (
        <section className="flex flex-col gap-3 rounded-2xl border border-red-300 bg-red-50 p-4 dark:border-red-900/50 dark:bg-red-950/30">
          <h2 className="text-lg font-semibold text-red-800 dark:text-red-300">Небезпечна зона</h2>
          <p className="text-sm text-red-800 dark:text-red-300">
            Архівування робить групу неактивною — нові діти не зможуть приєднатися за кодом. Група
            та її історія НЕ видаляються, її можна переглядати й надалі.
          </p>
          <button
            type="button"
            onClick={() => setConfirmArchiveOpen(true)}
            className="self-start rounded-full bg-red-700 px-4 py-2 text-white"
          >
            Архівувати групу
          </button>
        </section>
      ) : null}

      {confirmArchiveOpen ? (
        <div className="fixed inset-0 flex items-center justify-center bg-black/50 p-4">
          <div className="flex w-full max-w-sm flex-col gap-3 rounded-2xl bg-white p-6 dark:bg-neutral-900">
            <h3 className="text-lg font-semibold">Підтвердьте архівування</h3>
            <p className="text-sm">
              Група <strong>{group.name}</strong> стане неактивною. Нові діти не зможуть
              приєднатися за поточним кодом. Це не видаляє групу чи її результати.
            </p>
            {archiveError ? <p className="text-red-600">{archiveError.message}</p> : null}
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => {
                  setConfirmArchiveOpen(false);
                  setArchiveError(null);
                }}
                className="rounded-full border border-black/[.16] px-4 py-2 dark:border-white/[.2]"
              >
                Скасувати
              </button>
              <button
                type="button"
                onClick={onArchive}
                disabled={archiving}
                className="rounded-full bg-red-700 px-4 py-2 text-white disabled:opacity-50"
              >
                {archiving ? "Архівуємо…" : "Архівувати"}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </main>
  );
}

function AssignErrorMessage({ error }: { error: ApiError }) {
  if (error.code === "NOT_PUBLISHED") {
    return <p className="text-red-600">Цей урок ще не опубліковано.</p>;
  }
  return <p className="text-red-600">{error.message}</p>;
}
