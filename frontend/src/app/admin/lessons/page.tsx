"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import { apiFetch, ApiError, logout } from "@/lib/api";
import {
  AGE_BAND_LABELS,
  MAX_PATTERNS_PER_LESSON,
  moderationHref,
  PATTERNS,
  TOPIC_LABELS,
  VERSION_STATUS_LABELS,
  type AdminLessonEntry,
  type GenerationOutcome,
  type GenerationRequest,
  type LessonCreated,
} from "@/lib/admin-types";
import { adminRedirect } from "@/lib/two-factor";
import { AppHeader } from "@/components/ui/AppHeader";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Section, SectionHeader } from "@/components/ui/Card";
import { Field, Select, TextInput } from "@/components/ui/Field";
import { Pill } from "@/components/ui/Pill";
import { QuietButton, SecondaryButton, SecondaryLink } from "@/components/ui/SecondaryButton";

/** The only module in the MVP (docs/Обсяг_MVP.md) — shown as a fixed value, not a choice. */
const MODULE_CODE = "safety";
const MODULE_LABEL = "Безпека";

const FIELD_LABELS: Record<string, string> = {
  topic: "Тема",
  ageBand: "Вік",
  patternIds: "Патерни",
};

export default function AdminLessonsPage() {
  const router = useRouter();
  const [lessons, setLessons] = useState<AdminLessonEntry[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [generatingFor, setGeneratingFor] = useState<string | null>(null);

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

  const loadLessons = useCallback(() => {
    apiFetch<AdminLessonEntry[]>("/admin/lessons")
      .then((data) => {
        setLessons(data);
        setLoadError(null);
      })
      .catch((err) => {
        if (!handleError(err)) setLoadError("Не вдалося завантажити уроки.");
      });
  }, [handleError]);

  useEffect(() => {
    loadLessons();
  }, [loadLessons]);

  async function onLogout() {
    await logout();
    router.replace("/login");
  }

  return (
    <>
      <AppHeader context="Адміністратор" onLogout={onLogout} />
      <main className="flex flex-1 justify-center px-6 pt-10 pb-24">
        <div className="flex w-full max-w-260 flex-col gap-7">
          <div className="flex flex-col gap-2">
            <SecondaryLink href="/admin">← Адмін-кабінет</SecondaryLink>
            <h1 className="text-[32px] leading-[1.15] font-medium tracking-[-0.02em]">Уроки</h1>
            <p className="text-ink-muted text-base leading-relaxed">
              Створюйте уроки й генеруйте нові версії. Кожна версія проходить перевірку в модерації.
            </p>
          </div>

          <CreateLessonForm onCreated={loadLessons} onError={handleError} />

          <Section>
            <SectionHeader title="Усі уроки" aside={lessons ? `${lessons.length}` : undefined} />
            {loadError ? <p className="text-danger px-6 py-5 text-sm">{loadError}</p> : null}
            {!lessons && !loadError ? <p className="text-ink-muted px-6 py-5 text-sm">Завантаження…</p> : null}
            {lessons && lessons.length === 0 ? (
              <p className="text-ink-muted px-6 py-5 text-sm">Ще немає жодного уроку.</p>
            ) : null}
            {lessons?.map((lesson) => (
              <LessonRow
                key={lesson.id}
                lesson={lesson}
                generateOpen={generatingFor === lesson.id}
                onToggleGenerate={() => setGeneratingFor((current) => (current === lesson.id ? null : lesson.id))}
                onGenerated={loadLessons}
                onError={handleError}
              />
            ))}
          </Section>
        </div>
      </main>
    </>
  );
}

function CreateLessonForm({ onCreated, onError }: { onCreated: () => void; onError: (err: unknown) => boolean }) {
  const [title, setTitle] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [created, setCreated] = useState<string | null>(null);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (submitting) return;
    setSubmitting(true);
    setError(null);
    setCreated(null);
    try {
      const lesson = await apiFetch<LessonCreated>("/admin/lessons", {
        method: "POST",
        body: JSON.stringify({ moduleCode: MODULE_CODE, title: title.trim() }),
      });
      setCreated(lesson.title);
      setTitle("");
      onCreated();
    } catch (err) {
      if (!onError(err)) setError("Не вдалося створити урок.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Section>
      <SectionHeader title="Створити урок" />
      <form onSubmit={onSubmit} className="flex flex-col gap-4 px-6 pt-5 pb-6">
        {error ? <Alert title="Помилка">{error}</Alert> : null}
        {created ? <p className="text-ok-ink text-sm">Урок «{created}» створено.</p> : null}
        <div className="grid grid-cols-1 gap-4 md:grid-cols-[200px_1fr_auto] md:items-end">
          <Field label="Модуль">
            {({ id }) => <TextInput id={id} value={MODULE_LABEL} readOnly disabled />}
          </Field>
          <Field label="Назва">
            {({ id }) => (
              <TextInput
                id={id}
                required
                maxLength={200}
                placeholder="Напр.: М'яч на дорозі"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
              />
            )}
          </Field>
          <Button type="submit" disabled={submitting || title.trim().length === 0} className="h-[46px] md:w-auto md:px-6">
            {submitting ? "Створюємо…" : "Створити урок"}
          </Button>
        </div>
      </form>
    </Section>
  );
}

function LessonRow({
  lesson,
  generateOpen,
  onToggleGenerate,
  onGenerated,
  onError,
}: {
  lesson: AdminLessonEntry;
  generateOpen: boolean;
  onToggleGenerate: () => void;
  onGenerated: () => void;
  onError: (err: unknown) => boolean;
}) {
  const latest = lesson.latestVersion;
  const latestStatus = latest ? VERSION_STATUS_LABELS[latest.status] : null;
  return (
    <div className="border-line-row flex flex-col gap-4 border-b px-6 py-4.5 last:border-b-0">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div className="flex min-w-0 flex-col gap-1.5">
          <span className="text-[17px] font-medium">{lesson.title}</span>
          <span className="text-ink-icon flex flex-wrap items-center gap-3 text-sm">
            <span className="font-mono">{lesson.moduleCode}</span>
            <span>{versionCountLabel(lesson.versionCount)}</span>
            {latest && latestStatus ? (
              <span className="flex items-center gap-2">
                <span className="font-mono">v{latest.versionNo}</span>
                <Pill tone={latestStatus.tone}>{latestStatus.label}</Pill>
              </span>
            ) : null}
            {lesson.publishedVersionId ? <Pill tone="ok">Доступний дітям</Pill> : null}
          </span>
        </div>
        <div className="flex flex-wrap gap-2.5">
          {latest ? <SecondaryLink href={moderationHref(latest.id, latest.status)}>Переглянути</SecondaryLink> : null}
          <QuietButton type="button" onClick={onToggleGenerate} aria-expanded={generateOpen}>
            {generateOpen ? "Сховати" : "Згенерувати версію"}
          </QuietButton>
        </div>
      </div>
      {generateOpen ? <GenerateForm lessonId={lesson.id} onGenerated={onGenerated} onError={onError} /> : null}
    </div>
  );
}

function GenerateForm({
  lessonId,
  onGenerated,
  onError,
}: {
  lessonId: string;
  onGenerated: () => void;
  onError: (err: unknown) => boolean;
}) {
  const [topic, setTopic] = useState("road_safety");
  const [ageBand, setAgeBand] = useState("age_5_6");
  const [patternIds, setPatternIds] = useState<string[]>([]);
  const [title, setTitle] = useState("");
  const [learningGoal, setLearningGoal] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [outcome, setOutcome] = useState<GenerationOutcome | null>(null);
  const [error, setError] = useState<{ title: string; lines: string[] } | null>(null);
  // State updates are async, so a fast double click could still slip past `submitting`.
  const inFlight = useRef(false);

  const patterns = PATTERNS.filter((p) => p.topic === topic);

  function onTopicChange(next: string) {
    setTopic(next);
    setPatternIds((ids) => ids.filter((id) => PATTERNS.some((p) => p.id === id && p.topic === next)));
  }

  function togglePattern(id: string) {
    setPatternIds((ids) => (ids.includes(id) ? ids.filter((x) => x !== id) : [...ids, id]));
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (inFlight.current) return;
    inFlight.current = true;
    setSubmitting(true);
    setError(null);
    setOutcome(null);
    const body: GenerationRequest = {
      topic,
      ageBand,
      patternIds,
      ...(title.trim() ? { title: title.trim() } : {}),
      ...(learningGoal.trim() ? { learningGoal: learningGoal.trim() } : {}),
    };
    try {
      const result = await apiFetch<GenerationOutcome>(`/admin/lessons/${lessonId}/generate`, {
        method: "POST",
        body: JSON.stringify(body),
      });
      setOutcome(result);
      onGenerated();
    } catch (err) {
      if (!onError(err)) setError(generationErrorText(err));
    } finally {
      inFlight.current = false;
      setSubmitting(false);
    }
  }

  const outcomeStatus = outcome ? VERSION_STATUS_LABELS[outcome.status] : null;

  return (
    <form onSubmit={onSubmit} className="bg-canvas border-line flex flex-col gap-4 rounded-xl border p-5">
      {error ? (
        <Alert title={error.title}>
          {error.lines.length > 0 ? (
            <ul className="list-disc pl-4">
              {error.lines.map((line) => (
                <li key={line}>{line}</li>
              ))}
            </ul>
          ) : null}
        </Alert>
      ) : null}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <Field label="Тема">
          {({ id }) => (
            <Select id={id} value={topic} onChange={(e) => onTopicChange(e.target.value)} disabled={submitting}>
              {Object.entries(TOPIC_LABELS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </Select>
          )}
        </Field>
        <Field label="Вік">
          {({ id }) => (
            <Select id={id} value={ageBand} onChange={(e) => setAgeBand(e.target.value)} disabled={submitting}>
              {Object.entries(AGE_BAND_LABELS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </Select>
          )}
        </Field>
      </div>

      <fieldset className="flex flex-col gap-2.5" disabled={submitting}>
        <legend className="mb-1 text-[15px] font-medium">
          Патерни <span className="text-ink-soft text-[13px] font-normal">(1–{MAX_PATTERNS_PER_LESSON})</span>
        </legend>
        {patterns.map((pattern) => {
          const checked = patternIds.includes(pattern.id);
          const limitReached = !checked && patternIds.length >= MAX_PATTERNS_PER_LESSON;
          return (
            <label
              key={pattern.id}
              className={`flex items-center gap-2.5 text-[15px] ${limitReached ? "text-ink-faint" : "cursor-pointer"}`}
            >
              <input
                type="checkbox"
                checked={checked}
                disabled={limitReached}
                onChange={() => togglePattern(pattern.id)}
                className="size-4.5 accent-[color:var(--color-brand)]"
              />
              <span className="font-mono text-sm">{pattern.id}</span>
              <span>{pattern.name}</span>
            </label>
          );
        })}
      </fieldset>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <Field label="Назва версії" hint="Необов'язково">
          {({ id, describedBy }) => (
            <TextInput
              id={id}
              aria-describedby={describedBy}
              maxLength={60}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              disabled={submitting}
            />
          )}
        </Field>
        <Field label="Навчальна мета" hint="Необов'язково, як дія дитини">
          {({ id, describedBy }) => (
            <TextInput
              id={id}
              aria-describedby={describedBy}
              maxLength={200}
              value={learningGoal}
              onChange={(e) => setLearningGoal(e.target.value)}
              disabled={submitting}
            />
          )}
        </Field>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <SecondaryButton
          type="submit"
          disabled={submitting || patternIds.length === 0}
          className="h-11 gap-2.5 px-5 text-[15px] disabled:cursor-default disabled:opacity-50"
        >
          {submitting ? (
            <>
              <span
                aria-hidden="true"
                className="border-brand size-4 animate-spin rounded-full border-2 border-t-transparent"
              />
              Генеруємо… це може тривати до хвилини
            </>
          ) : (
            "Згенерувати"
          )}
        </SecondaryButton>
      </div>

      {outcome && outcomeStatus ? (
        <div
          aria-live="polite"
          className="bg-surface border-line flex flex-col gap-2.5 rounded-lg border px-4 py-3.5 text-sm"
        >
          <div className="flex flex-wrap items-center gap-2.5">
            <span className="font-medium">Результат:</span>
            <Pill tone={outcomeStatus.tone}>{outcomeStatus.label}</Pill>
            <span className="text-ink-icon">спроб: {outcome.attempts}</span>
          </div>
          {outcome.problems.length > 0 ? (
            <ul className="text-danger-body list-disc pl-5 leading-relaxed">
              {outcome.problems.map((p) => (
                <li key={`${p.path}:${p.message}`}>
                  <span className="font-mono">{p.path || "/"}</span> — {p.message}
                </li>
              ))}
            </ul>
          ) : null}
          <Link href={moderationHref(outcome.versionId, outcome.status)} className="self-start font-medium">
            {outcome.status === "auto_validated" ? "Перевірити версію →" : "Переглянути версію →"}
          </Link>
        </div>
      ) : null}
    </form>
  );
}

function generationErrorText(err: unknown): { title: string; lines: string[] } {
  if (err instanceof ApiError) {
    if (err.status === 400 && err.problems?.length) {
      return {
        title: "Перевірте поля форми",
        lines: err.problems.map((p) => `${FIELD_LABELS[p.path.split("/")[0]] ?? p.path}: ${p.message}`),
      };
    }
    if (err.status === 422 && err.code === "LLM_REFUSED") {
      return { title: "Модель відмовилась генерувати цей урок", lines: ["Змініть тему чи патерни й спробуйте ще раз."] };
    }
    if (err.status === 502 || err.status === 503 || err.status === 504) {
      return {
        title: "Сервіс генерації зараз недоступний",
        lines: ["Спробуйте ще раз за кілька хвилин. Версію не створено."],
      };
    }
    if (err.status === 404) return { title: "Урок не знайдено", lines: [] };
  }
  return { title: "Не вдалося згенерувати версію", lines: [] };
}

/** «1 версія / 3 версії / 5 версій». */
function versionCountLabel(n: number): string {
  const mod10 = n % 10;
  const mod100 = n % 100;
  const word =
    mod100 >= 11 && mod100 <= 14 ? "версій" : mod10 === 1 ? "версія" : mod10 >= 2 && mod10 <= 4 ? "версії" : "версій";
  return `${n} ${word}`;
}
