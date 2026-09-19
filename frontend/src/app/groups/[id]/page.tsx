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
import { AppHeader } from "@/components/ui/AppHeader";
import { Avatar, tintFor } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";
import { NoticeButton } from "@/components/ui/Notice";
import { Pill } from "@/components/ui/Pill";
import { Section, SectionHeader } from "@/components/ui/Card";
import { QuietButton } from "@/components/ui/SecondaryButton";
import { Select } from "@/components/ui/Field";

const RESULTS: Record<string, { label: string; tone: "ok" | "bad" }> = {
  completed: { label: "Завершено", tone: "ok" },
  abandoned: { label: "Перервано", tone: "bad" },
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
      .catch((err) => setGroupError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка")))
      .finally(() => setGroupLoading(false));
  }, [id]);

  const loadMembers = useCallback(() => {
    apiFetch<GroupMemberInfo[]>(`/groups/${id}/members`)
      .then(setMembers)
      .catch((err) => setMembersError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка")));
  }, [id]);

  const loadAssignments = useCallback(() => {
    apiFetch<Assignment[]>(`/assignments?groupId=${id}`)
      .then(setAssignments)
      .catch((err) => setAssignmentsError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка")));
  }, [id]);

  const loadResults = useCallback(() => {
    apiFetch<TeacherResultChild[]>(`/groups/${id}/results`)
      .then(setResults)
      .catch((err) => setResultsError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка")));
  }, [id]);

  const loadCatalog = useCallback(() => {
    apiFetch<CatalogEntry[]>("/catalog/lessons")
      .then(setCatalog)
      .catch((err) => setCatalogError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка")));
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
      setGroupError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка"));
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
      setMembersError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка"));
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
      setAssignError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка"));
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
      setAssignmentsError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка"));
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
      setArchiveError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка"));
    } finally {
      setArchiving(false);
    }
  }

  function catalogTitleFor(lessonVersionId: string): string {
    const entry = catalog?.find((c) => c.currentVersionId === lessonVersionId);
    return entry ? entry.title : lessonVersionId;
  }

  function catalogModuleFor(lessonVersionId: string): string | null {
    return catalog?.find((c) => c.currentVersionId === lessonVersionId)?.moduleCode ?? null;
  }

  if (groupLoading) {
    return (
      <>
        <AppHeader context="Кабінет вчителя" />
        <main className="flex flex-1 items-center justify-center p-8">
          <p className="text-ink-muted">Завантаження…</p>
        </main>
      </>
    );
  }

  if (groupError || !group) {
    return (
      <>
        <AppHeader context="Кабінет вчителя" />
        <main className="flex flex-1 flex-col items-center justify-center gap-3 p-8">
          <p className="text-danger">
            {groupError?.status === 404 ? "Групу не знайдено." : (groupError?.message ?? "Групу не знайдено.")}
          </p>
          <Link href="/groups" className="text-sm">
            ← До груп
          </Link>
        </main>
      </>
    );
  }

  const assignedVersionIds = new Set(assignments?.map((a) => a.lessonVersionId) ?? []);
  const availableCatalog = catalog?.filter((c) => !assignedVersionIds.has(c.currentVersionId)) ?? [];
  const resultRows = (results ?? []).flatMap((child) =>
    child.attempts.map((attempt, i) => ({ key: `${child.childId}-${i}`, child: child.displayName, attempt })),
  );

  return (
    <>
      <AppHeader context="Кабінет вчителя" />

      <main className="flex flex-1 justify-center px-6 pt-7 pb-24">
        <div className="flex w-full max-w-260 flex-col gap-6">
          <Link href="/groups" className="text-ink-muted text-sm no-underline hover:underline">
            ← Мої групи
          </Link>

          <div className="flex flex-col gap-2.25">
            <div className="flex flex-wrap items-center gap-3">
              <h1 className="text-[30px] leading-[1.15] font-medium tracking-[-0.02em]">{group.name}</h1>
              <Pill tone={group.isActive ? "ok" : "neutral"}>{group.isActive ? "Активна" : "Архів"}</Pill>
            </div>
            <p className="text-ink-muted text-[15px]">
              {members ? membersLabel(members.length) : "…"} ·{" "}
              {assignments ? assignmentsLabel(assignments.length) : "…"}
            </p>
          </div>

          {group.isActive ? (
            <section className="bg-surface border-line shadow-card flex flex-wrap items-center justify-between gap-5 rounded-xl border px-6 py-5.5">
              <div className="flex flex-col gap-2">
                <span className="text-ink-icon font-mono text-xs tracking-[0.09em] uppercase">
                  Код приєднання
                </span>
                <span className="text-brand font-mono text-[38px] leading-none font-medium tracking-[0.14em]">
                  {group.joinCode}
                </span>
                <span className="text-ink-icon text-[13px]">
                  Батьки вводять код у профілі дитини.
                </span>
              </div>
              <div className="flex flex-wrap items-center gap-2.5">
                <Button type="button" onClick={onCopy} className="h-11 w-auto px-5 text-[15px]">
                  {copied ? "Скопійовано" : "Копіювати"}
                </Button>
                <QuietButton
                  type="button"
                  onClick={onRegenerateCode}
                  disabled={regenerating}
                  className="h-11 px-4.5 text-[15px]"
                >
                  {regenerating ? "Оновлюємо…" : "Згенерувати новий код"}
                </QuietButton>
              </div>
            </section>
          ) : (
            <p className="bg-surface border-line text-ink-soft rounded-xl border px-6 py-5 text-sm">
              Ця група архівна — нові діти не можуть приєднатися за кодом.
            </p>
          )}

          <Section>
            <SectionHeader
              title="Учні"
              description="Вчитель бачить лише ім'я та прогрес дитини."
              aside={members ? membersLabel(members.length) : undefined}
            />

            {membersError ? <p className="text-danger px-6 py-4 text-sm">{membersError.message}</p> : null}
            {!members && !membersError ? (
              <p className="text-ink-muted px-6 py-4 text-sm">Завантаження…</p>
            ) : null}
            {members && members.length === 0 ? (
              <p className="text-ink-muted px-6 py-4 text-sm">
                Ще ніхто не приєднався. Поділіться кодом{" "}
                <span className="text-ink font-mono">{group.joinCode}</span>.
              </p>
            ) : null}

            {members && members.length > 0 ? (
              <div className="grid grid-cols-[repeat(auto-fill,minmax(280px,1fr))] gap-3 px-6 pt-5 pb-6">
                {members.map((member) => (
                  <div
                    key={member.childId}
                    className="border-line-head flex items-center gap-3 rounded-xl border px-3.5 py-3"
                  >
                    <Avatar name={member.displayName} tint={tintFor(member.childId)} />
                    <span className="min-w-0 flex-1 text-[15px] font-medium">{member.displayName}</span>
                    <QuietButton
                      type="button"
                      onClick={() => onRemoveMember(member.childId)}
                      disabled={removingChildId === member.childId}
                      className="hover:border-danger-line-soft hover:bg-danger-surface hover:text-danger-title h-8.5 px-3"
                    >
                      {removingChildId === member.childId ? "Вилучаємо…" : "Вилучити"}
                    </QuietButton>
                  </div>
                ))}
              </div>
            ) : null}
          </Section>

          <Section>
            <SectionHeader
              title="Призначені уроки"
              description="Учні бачать призначені уроки у своєму кабінеті."
            />

            <form
              onSubmit={onAssign}
              className="bg-row-hover border-line-soft flex flex-wrap items-end gap-2.5 border-b px-6 py-5"
            >
              <div className="flex min-w-60 flex-1 flex-col gap-[7px]">
                <label htmlFor="lesson" className="text-[15px] font-medium">
                  Призначити урок
                </label>
                <Select
                  id="lesson"
                  required
                  className="border-line-field"
                  value={selectedVersionId}
                  onChange={(e) => setSelectedVersionId(e.target.value)}
                >
                  <option value="" disabled>
                    Оберіть урок з каталогу
                  </option>
                  {availableCatalog.map((entry) => (
                    <option key={entry.currentVersionId} value={entry.currentVersionId}>
                      {entry.title} ({entry.moduleCode})
                    </option>
                  ))}
                </Select>
              </div>
              <Button
                type="submit"
                disabled={assigning || !selectedVersionId}
                className="h-11 w-auto flex-none px-5 text-[15px]"
              >
                {assigning ? "Призначаємо…" : "Призначити"}
              </Button>
            </form>

            {catalogError ? <p className="text-danger px-6 py-3 text-sm">{catalogError.message}</p> : null}
            {assignError ? (
              <p className="text-danger px-6 py-3 text-sm">{assignErrorText(assignError)}</p>
            ) : null}
            {catalog && catalog.length === 0 ? (
              <p className="text-ink-soft px-6 py-3 text-sm">У каталозі ще немає опублікованих уроків.</p>
            ) : null}

            {assignmentsError ? (
              <p className="text-danger px-6 py-4 text-sm">{assignmentsError.message}</p>
            ) : null}
            {!assignments && !assignmentsError ? (
              <p className="text-ink-muted px-6 py-4 text-sm">Завантаження…</p>
            ) : null}
            {assignments && assignments.length === 0 ? (
              <p className="text-ink-muted px-6 py-4 text-sm">Ще немає призначених уроків.</p>
            ) : null}

            {assignments?.map((assignment) => (
              <div
                key={assignment.id}
                className="border-line-soft flex items-center justify-between gap-4 border-b px-6 py-4 last:border-b-0"
              >
                <div className="flex min-w-0 flex-col gap-0.75">
                  <span className="font-medium">{catalogTitleFor(assignment.lessonVersionId)}</span>
                  <span className="text-ink-icon text-sm">
                    {catalogModuleFor(assignment.lessonVersionId) ?? "Модуль невідомий"}
                    {assignment.dueAt
                      ? ` · до ${new Date(assignment.dueAt).toLocaleDateString("uk-UA")}`
                      : ""}
                  </span>
                </div>
                <QuietButton
                  type="button"
                  onClick={() => onRemoveAssignment(assignment.id)}
                  disabled={removingAssignmentId === assignment.id}
                >
                  {removingAssignmentId === assignment.id ? "Прибираємо…" : "Прибрати"}
                </QuietButton>
              </div>
            ))}
          </Section>

          <Section>
            <SectionHeader
              title="Результати"
              description="Останні спроби учнів за призначеними уроками."
            />

            {resultsError ? <p className="text-danger px-6 py-4 text-sm">{resultsError.message}</p> : null}
            {!results && !resultsError ? (
              <p className="text-ink-muted px-6 py-4 text-sm">Завантаження…</p>
            ) : null}
            {results && resultRows.length === 0 ? (
              <p className="text-ink-muted px-6 py-4 text-sm">Ще немає результатів.</p>
            ) : null}

            {resultRows.length > 0 ? (
              <div className="overflow-x-auto">
                <table className="w-full border-collapse text-[15px]">
                  <thead>
                    <tr className="bg-row-hover text-ink-faint border-line-head border-b font-mono text-xs tracking-[0.08em] uppercase">
                      <th className="px-6 py-3 text-left font-medium">Учень</th>
                      <th className="px-4 py-3 text-left font-medium">Урок</th>
                      <th className="px-4 py-3 text-left font-medium">Дата</th>
                      <th className="px-4 py-3 text-left font-medium">Результат</th>
                      <th className="px-6 py-3 text-right font-medium">Бали</th>
                    </tr>
                  </thead>
                  <tbody>
                    {resultRows.map(({ key, child, attempt }) => {
                      const result = attempt.result ? RESULTS[attempt.result] : null;
                      return (
                        <tr key={key} className="border-line-row border-b">
                          <td className="px-6 py-3.5 font-medium">{child}</td>
                          <td className="text-avatar-ink px-4 py-3.5">{attempt.title}</td>
                          <td className="text-ink-icon px-4 py-3.5 font-mono text-sm">
                            {attempt.completedAt
                              ? new Date(attempt.completedAt).toLocaleDateString("uk-UA")
                              : "—"}
                          </td>
                          <td className="px-4 py-3.5">
                            <Pill tone={result ? result.tone : "waiting"}>
                              {result ? result.label : "У процесі"}
                            </Pill>
                          </td>
                          <td className="text-avatar-ink px-6 py-3.5 text-right font-mono">
                            {attempt.score != null ? `${Math.round(attempt.score * 100)}%` : "—"}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            ) : null}
          </Section>

          {group.isActive ? (
            <section className="bg-surface border-notice-line flex flex-wrap items-center justify-between gap-5 rounded-xl border px-6 py-5.5">
              <div className="flex min-w-65 flex-1 flex-col gap-1.25">
                <h2 className="text-notice-title text-lg font-medium">Архівувати групу</h2>
                <p className="text-ink-soft text-sm leading-relaxed text-pretty">
                  Код приєднання перестане працювати, нові діти не зможуть приєднатися. Група та її
                  результати НЕ видаляються — їх можна переглядати й надалі.
                </p>
              </div>
              <NoticeButton type="button" onClick={() => setConfirmArchiveOpen(true)}>
                Архівувати групу
              </NoticeButton>
            </section>
          ) : null}
        </div>
      </main>

      {confirmArchiveOpen ? (
        <Modal
          title="Підтвердьте архівування"
          onClose={() => {
            setConfirmArchiveOpen(false);
            setArchiveError(null);
          }}
          footer={
            <>
              <QuietButton
                type="button"
                className="h-11 px-4 text-[15px]"
                onClick={() => {
                  setConfirmArchiveOpen(false);
                  setArchiveError(null);
                }}
              >
                Скасувати
              </QuietButton>
              <NoticeButton type="button" onClick={onArchive} disabled={archiving}>
                {archiving ? "Архівуємо…" : "Архівувати"}
              </NoticeButton>
            </>
          }
        >
          <p className="text-ink-soft text-sm leading-relaxed text-pretty">
            Група <strong className="text-ink font-medium">{group.name}</strong> стане неактивною.
            Нові діти не зможуть приєднатися за поточним кодом. Це не видаляє групу чи її
            результати.
          </p>
          {archiveError ? <p className="text-danger text-sm">{archiveError.message}</p> : null}
        </Modal>
      ) : null}
    </>
  );
}

function assignErrorText(error: ApiError): string {
  if (error.code === "NOT_PUBLISHED") return "Цей урок ще не опубліковано.";
  return error.message;
}

function membersLabel(n: number): string {
  const mod100 = n % 100;
  const mod10 = n % 10;
  if (mod100 >= 11 && mod100 <= 14) return `${n} учнів`;
  if (mod10 === 1) return `${n} учень`;
  if (mod10 >= 2 && mod10 <= 4) return `${n} учні`;
  return `${n} учнів`;
}

function assignmentsLabel(n: number): string {
  const mod100 = n % 100;
  const mod10 = n % 10;
  if (mod100 >= 11 && mod100 <= 14) return `${n} призначених уроків`;
  if (mod10 === 1) return `${n} призначений урок`;
  if (mod10 >= 2 && mod10 <= 4) return `${n} призначені уроки`;
  return `${n} призначених уроків`;
}
