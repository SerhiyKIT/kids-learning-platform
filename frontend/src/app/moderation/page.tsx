"use client";

import { useCallback, useEffect, useState } from "react";
import { apiFetch, ApiError } from "@/lib/api";
import type { LessonVersionDetail, PendingReviewEntry } from "@/lib/admin-types";
import type { Scene } from "@/lib/scenario-types";
import { isChoiceScene, isDemoScene } from "@/lib/scenario-types";
import { AppHeader } from "@/components/ui/AppHeader";
import { Button } from "@/components/ui/Button";
import { Pill } from "@/components/ui/Pill";
import { QuietButton } from "@/components/ui/SecondaryButton";

const STATUS_LABELS: Record<string, { label: string; tone: "ok" | "waiting" | "bad" }> = {
  auto_validated: { label: "auto_validated", tone: "waiting" },
  approved: { label: "approved", tone: "ok" },
  published: { label: "published", tone: "ok" },
  rejected_auto: { label: "rejected_auto", tone: "bad" },
  archived: { label: "archived", tone: "bad" },
};

export default function ModerationPage() {
  const [queue, setQueue] = useState<PendingReviewEntry[] | null>(null);
  const [queueError, setQueueError] = useState<ApiError | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const [detail, setDetail] = useState<LessonVersionDetail | null>(null);
  const [detailError, setDetailError] = useState<ApiError | null>(null);

  const [acting, setActing] = useState<"approve" | "publish" | "reject" | null>(null);
  const [actionError, setActionError] = useState<ApiError | null>(null);
  const [rejectOpen, setRejectOpen] = useState(false);
  const [reason, setReason] = useState("");
  const [approvedToday, setApprovedToday] = useState(0);

  const loadQueue = useCallback(() => {
    apiFetch<PendingReviewEntry[]>("/admin/lesson-versions?status=auto_validated")
      .then((data) => {
        setQueue(data);
        setSelectedId((current) => current ?? data[0]?.versionId ?? null);
      })
      .catch((err) => setQueueError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка")));
  }, []);

  useEffect(() => {
    loadQueue();
  }, [loadQueue]);

  useEffect(() => {
    if (!selectedId) return;
    // eslint-disable-next-line react-hooks/set-state-in-effect -- resetting detail panel when selection changes
    setDetail(null);
    setDetailError(null);
    apiFetch<LessonVersionDetail>(`/admin/lesson-versions/${selectedId}`)
      .then(setDetail)
      .catch((err) => setDetailError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка")));
  }, [selectedId]);

  async function act(kind: "approve" | "publish" | "reject") {
    if (!selectedId) return;
    setActing(kind);
    setActionError(null);
    try {
      const body = kind === "reject" ? JSON.stringify({ reason }) : undefined;
      const updated = await apiFetch<{ status: LessonVersionDetail["status"] }>(
        `/admin/lesson-versions/${selectedId}/${kind}`,
        { method: "POST", body },
      );
      setDetail((d) => (d ? { ...d, status: updated.status } : d));
      if (kind === "approve") setApprovedToday((n) => n + 1);
      if (kind === "reject") {
        setRejectOpen(false);
        setReason("");
      }
      // Схвалений/відхилений урок виходить із черги auto_validated — перечитуємо її.
      if (kind !== "publish") {
        setSelectedId(null);
        setQueue(null);
        loadQueue();
      }
    } catch (err) {
      setActionError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка"));
    } finally {
      setActing(null);
    }
  }

  const status = detail ? (STATUS_LABELS[detail.status] ?? { label: detail.status, tone: "waiting" as const }) : null;
  const isApproved = detail?.status === "approved";
  const isPending = detail?.status === "auto_validated";

  return (
    <>
      <AppHeader context="Модерація контенту" />

      <main className="flex flex-1 flex-col gap-5.5 px-8 pt-7 pb-18">
        <div className="flex flex-wrap items-end justify-between gap-5">
          <div className="flex flex-col gap-2">
            <h1 className="text-[30px] leading-[1.15] font-medium tracking-[-0.02em]">Черга перевірки</h1>
            <p className="text-ink-muted text-base">
              Уроки, що пройшли автоматичну валідацію і чекають на рішення модератора.
            </p>
          </div>
          <div className="flex gap-2.5">
            <Metric label="У черзі" value={queue ? String(queue.length) : "—"} />
            <Metric label="Схвалено за сесію" value={String(approvedToday)} />
          </div>
        </div>

        <div className="grid grid-cols-1 items-start gap-5 xl:grid-cols-[minmax(360px,1fr)_minmax(420px,1.05fr)]">
          <section className="bg-surface border-line shadow-card overflow-hidden rounded-xl border">
            <div className="bg-row-hover text-ink-faint border-line-head grid grid-cols-[1fr_62px_92px] gap-2.5 border-b px-4.5 py-3 font-mono text-xs tracking-[0.08em] uppercase">
              <span>Урок</span>
              <span>Версія</span>
              <span>Автор</span>
            </div>

            {queueError ? <p className="text-danger px-4.5 py-4 text-sm">{queueError.message}</p> : null}
            {!queue && !queueError ? (
              <p className="text-ink-muted px-4.5 py-4 text-sm">Завантаження…</p>
            ) : null}
            {queue && queue.length === 0 ? (
              <p className="text-ink-muted px-4.5 py-5 text-sm">Черга порожня — усе перевірено.</p>
            ) : null}

            {queue?.map((entry) => {
              const selected = entry.versionId === selectedId;
              const entryStatus = STATUS_LABELS[entry.status];
              return (
                <button
                  key={entry.versionId}
                  type="button"
                  onClick={() => setSelectedId(entry.versionId)}
                  aria-current={selected}
                  className={`border-line-row grid w-full cursor-pointer grid-cols-[1fr_62px_92px] items-center gap-2.5 border-b border-l-3 px-4.5 py-3.5 text-left transition-colors duration-100 ${
                    selected
                      ? "border-l-brand bg-brand-tint"
                      : "bg-surface hover:bg-row-hover border-l-transparent"
                  }`}
                >
                  <span className="flex min-w-0 flex-col gap-1.25">
                    <span className="text-[15px] font-medium">{entry.title}</span>
                    {entryStatus ? <Pill tone={entryStatus.tone}>{entryStatus.label}</Pill> : null}
                  </span>
                  <span className="text-ink-icon font-mono text-sm">v{entry.versionNo}</span>
                  <AuthorTag generatedBy={entry.generatedBy} />
                </button>
              );
            })}
          </section>

          <section className="bg-surface border-line shadow-card flex flex-col overflow-hidden rounded-xl border xl:sticky xl:top-6">
            {detailError ? <p className="text-danger px-6 py-5 text-sm">{detailError.message}</p> : null}
            {!detail && !detailError ? (
              <p className="text-ink-muted px-6 py-5 text-sm">
                {selectedId ? "Завантаження сценарію…" : "Виберіть урок зі черги."}
              </p>
            ) : null}

            {detail ? (
              <>
                <div className="border-line-soft flex flex-col gap-2.5 border-b px-6 pt-5 pb-4.5">
                  <div className="flex flex-wrap items-center gap-2.5">
                    <h2 className="text-[21px] font-medium tracking-[-0.01em]">
                      {detail.scenario.title}
                    </h2>
                    {status ? <Pill tone={status.tone}>{status.label}</Pill> : null}
                  </div>
                  <div className="text-ink-icon flex flex-wrap gap-4.5 font-mono text-[13px]">
                    <span>{detail.scenario.module}</span>
                    <span>v{detail.versionNo}</span>
                    <span>{detail.generatedBy}</span>
                    <span>{new Date(detail.createdAt).toLocaleString("uk-UA")}</span>
                  </div>
                </div>

                <div className="flex max-h-115 flex-col gap-4 overflow-y-auto px-6 pt-5 pb-5.5">
                  <span className="text-ink-faint font-mono text-xs tracking-[0.09em] uppercase">
                    Сценарій уроку
                  </span>
                  <div className="flex flex-col gap-1.25">
                    <span className="text-ink-icon text-[13px]">Навчальна мета</span>
                    <p className="text-[15px] leading-relaxed text-pretty">
                      {detail.scenario.learning_goal}
                    </p>
                  </div>

                  {detail.scenario.scenes.map((scene) => (
                    <SceneCard key={scene.key} scene={scene} />
                  ))}
                </div>

                <div className="bg-row-hover border-line-soft flex flex-col gap-3.5 border-t px-6 pt-5 pb-6">
                  {actionError ? <p className="text-danger text-sm">{actionError.message}</p> : null}

                  {isApproved ? (
                    <>
                      <div className="bg-ok-surface border-ok-line flex items-center gap-2.75 rounded-lg border px-3.75 py-3.25">
                        <span
                          aria-hidden="true"
                          className="bg-mark-ok flex size-5 flex-none items-center justify-center rounded-full text-[11px] font-semibold text-white"
                        >
                          ✓
                        </span>
                        <p className="text-ok-ink text-sm leading-relaxed">
                          Урок схвалено. Публікація зробить його доступним для призначення у групах.
                        </p>
                      </div>
                      <Button
                        type="button"
                        onClick={() => act("publish")}
                        disabled={acting !== null}
                        className="bg-mark-ok hover:bg-mark-ok/90 active:bg-mark-ok/80 h-13 text-[17px]"
                      >
                        {acting === "publish" ? "Публікуємо…" : "Опублікувати"}
                      </Button>
                    </>
                  ) : null}

                  {isPending ? (
                    <div className="flex flex-col gap-2.5">
                      <Button
                        type="button"
                        onClick={() => act("approve")}
                        disabled={acting !== null}
                        className="h-14 text-lg"
                      >
                        {acting === "approve" ? "Схвалюємо…" : "Схвалити"}
                      </Button>
                      <button
                        type="button"
                        onClick={() => setRejectOpen((v) => !v)}
                        className="text-danger-title border-danger-line-soft bg-surface hover:bg-danger-surface h-11 cursor-pointer rounded-lg border text-[15px] font-medium transition-colors duration-100"
                      >
                        Відхилити
                      </button>
                    </div>
                  ) : null}

                  {isPending && rejectOpen ? (
                    <div className="bg-surface border-danger-line-soft flex flex-col gap-2.25 rounded-lg border p-4">
                      <label htmlFor="reason" className="text-danger-title text-[15px] font-medium">
                        Причина відхилення
                      </label>
                      <textarea
                        id="reason"
                        rows={4}
                        required
                        value={reason}
                        onChange={(e) => setReason(e.target.value)}
                        placeholder="Напр.: сцена 3 пропонує дитині зустрітись — суперечить методиці."
                        className="bg-surface border-line-field text-ink w-full resize-y rounded-lg border px-3.25 py-2.75 text-[15px] leading-relaxed"
                      />
                      <p className="text-ink-soft text-[13px]">
                        Причина повертається автору разом із версією уроку.
                      </p>
                      <div className="mt-1 flex gap-2.5">
                        <button
                          type="button"
                          onClick={() => act("reject")}
                          disabled={acting !== null || reason.trim().length === 0}
                          className="bg-danger hover:bg-danger-hover h-11 flex-1 cursor-pointer rounded-lg text-[15px] font-medium text-white transition-colors duration-100 disabled:opacity-50"
                        >
                          {acting === "reject" ? "Надсилаємо…" : "Надіслати відхилення"}
                        </button>
                        <QuietButton
                          type="button"
                          onClick={() => setRejectOpen(false)}
                          className="h-11 px-4.5 text-[15px]"
                        >
                          Скасувати
                        </QuietButton>
                      </div>
                    </div>
                  ) : null}
                </div>
              </>
            ) : null}
          </section>
        </div>
      </main>
    </>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="bg-surface border-line rounded-xl border px-4 py-2.5">
      <div className="text-ink-faint font-mono text-xs tracking-[0.08em] uppercase">{label}</div>
      <div className="text-[22px] font-medium">{value}</div>
    </div>
  );
}

/** AI-generated versions read differently from human ones — the tag makes that scannable. */
function AuthorTag({ generatedBy }: { generatedBy: string }) {
  const isAi = generatedBy.toLowerCase().includes("ai");
  return (
    <span
      className={`justify-self-start rounded-full border px-2 py-0.75 font-mono text-xs tracking-[0.06em] uppercase ${
        isAi
          ? "border-[#d4cfe8] bg-[#f0eef8] text-[#4a3f7a]"
          : "border-line-chip bg-hover-soft text-avatar-ink"
      }`}
    >
      {isAi ? "AI" : "Human"}
    </span>
  );
}

function SceneCard({ scene }: { scene: Scene }) {
  return (
    <div className="border-line-head flex flex-col gap-2.25 rounded-xl border bg-[#fbfcfd] p-3.5">
      <div className="flex items-center gap-2.25">
        <span className="text-brand border-brand-line rounded-full border bg-[#eef5f6] px-2.25 py-0.5 font-mono text-xs">
          {scene.key}
        </span>
        <span className="text-[15px] font-medium">{sceneTypeLabel(scene)}</span>
      </div>

      {isChoiceScene(scene) ? (
        <>
          <p className="text-avatar-ink text-sm leading-relaxed text-pretty">{scene.setup.text}</p>
          <div className="flex flex-col gap-1.5">
            {scene.options.map((option) => (
              <div key={option.id} className="flex items-start gap-2.25 text-sm leading-relaxed">
                <span
                  aria-hidden="true"
                  className={`mt-0.5 flex size-4 flex-none items-center justify-center rounded-full text-[10px] font-semibold text-white ${
                    option.correct ? "bg-mark-ok" : "bg-danger"
                  }`}
                >
                  {option.correct ? "✓" : "✕"}
                </span>
                <span>{option.label.text}</span>
              </div>
            ))}
          </div>
          {scene.assistant.hints.length > 0 ? (
            <p className="text-ink-icon text-[13px] leading-relaxed">
              Підказка: {scene.assistant.hints[0].line.text}
            </p>
          ) : null}
        </>
      ) : isDemoScene(scene) ? (
        <p className="text-avatar-ink text-sm leading-relaxed text-pretty">
          {scene.narration.map((n) => n.line.text).join(" ")}
        </p>
      ) : (
        <p className="text-ink-icon text-sm leading-relaxed">
          Тип сцени «{scene.type ?? "невідомо"}» — перегляньте JSON сценарію перед схваленням.
        </p>
      )}
    </div>
  );
}

function sceneTypeLabel(scene: Scene): string {
  switch (scene.type) {
    case "demo":
      return "Демонстрація";
    case "choice":
      return "Вибір варіанта";
    case "sorting":
      return "Сортування";
    case "dialog":
      return "Діалог";
    default:
      return "Сцена";
  }
}
