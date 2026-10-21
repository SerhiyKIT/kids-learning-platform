"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useCallback, useEffect, useRef, useState } from "react";
import { apiFetch, ApiError } from "@/lib/api";
import {
  moderationTab,
  VERSION_STATUS_LABELS,
  type LessonVersionDetail,
  type LessonVersionStatus,
  type PendingReviewEntry,
  type VoicingResult,
} from "@/lib/admin-types";
import type { Scene } from "@/lib/scenario-types";
import { isChoiceScene, isDemoScene } from "@/lib/scenario-types";
import { adminRedirect } from "@/lib/two-factor";
import { AppHeader } from "@/components/ui/AppHeader";
import { Button } from "@/components/ui/Button";
import { Notice, NoticeButton } from "@/components/ui/Notice";
import { Pill } from "@/components/ui/Pill";
import { QuietButton, SecondaryButton, SecondaryLink } from "@/components/ui/SecondaryButton";

type TabKey = "pending" | "approved" | "published" | "rejected";

/** «Відхилені» holds both kinds of rejection: by the validator (rejected_auto) and by a moderator (archived). */
const TABS: { key: TabKey; label: string; statuses: LessonVersionStatus[] }[] = [
  { key: "pending", label: "На перевірці", statuses: ["auto_validated"] },
  { key: "approved", label: "Схвалені", statuses: ["approved"] },
  { key: "published", label: "Опубліковані", statuses: ["published"] },
  { key: "rejected", label: "Відхилені", statuses: ["rejected_auto", "archived"] },
];

type Lists = Record<TabKey, PendingReviewEntry[]>;
type Action = "approve" | "reject" | "publish" | "voice";

function isTabKey(value: string | null): value is TabKey {
  return TABS.some((t) => t.key === value);
}

function loadLists(): Promise<Lists> {
  const statuses = TABS.flatMap((t) => t.statuses);
  return Promise.all(
    statuses.map((s) => apiFetch<PendingReviewEntry[]>(`/admin/lesson-versions?status=${s}`)),
  ).then((results) => {
    const byStatus = new Map(statuses.map((s, i) => [s, results[i]]));
    return Object.fromEntries(
      TABS.map((t) => [t.key, t.statuses.flatMap((s) => byStatus.get(s) ?? [])]),
    ) as Lists;
  });
}

function actionErrorText(action: Action, err: unknown): string {
  if (err instanceof ApiError) {
    if (action === "voice" && err.status === 409) {
      return "Озвучувати можна лише схвалені або опубліковані версії.";
    }
    if (action === "voice" && (err.status === 502 || err.status === 503)) {
      return "Сервіс озвучки зараз недоступний. Спробуйте пізніше.";
    }
    if (err.status === 409) return "Стан версії вже змінився. Оновіть сторінку.";
    return err.message;
  }
  return "Несподівана помилка";
}

export default function ModerationPage() {
  // useSearchParams() needs a Suspense boundary to keep the rest of the route prerenderable.
  return (
    <Suspense fallback={<AppHeader context="Модерація контенту" />}>
      <ModerationScreen />
    </Suspense>
  );
}

function ModerationScreen() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const initialVersion = searchParams.get("version");
  const initialTab = searchParams.get("status");

  const [tab, setTab] = useState<TabKey>(isTabKey(initialTab) ? initialTab : "pending");
  const [lists, setLists] = useState<Lists | null>(null);
  const [listError, setListError] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const [detail, setDetail] = useState<LessonVersionDetail | null>(null);
  const [detailError, setDetailError] = useState<string | null>(null);

  const [acting, setActing] = useState<Action | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [rejectOpen, setRejectOpen] = useState(false);
  const [reason, setReason] = useState("");
  const [publishWarningOpen, setPublishWarningOpen] = useState(false);
  const [voicedThisSession, setVoicedThisSession] = useState<Set<string>>(() => new Set());
  const [voicing, setVoicing] = useState<{ versionId: string; result: VoicingResult } | null>(null);
  // The version most recently asked for, so a slow detail response for an earlier click is dropped.
  const latestSelection = useRef<string | null>(null);

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

  const selectVersion = useCallback(
    (id: string | null) => {
      latestSelection.current = id;
      setSelectedId(id);
      setDetail(null);
      setDetailError(null);
      setActionError(null);
      setRejectOpen(false);
      setPublishWarningOpen(false);
      if (!id) return;
      apiFetch<LessonVersionDetail>(`/admin/lesson-versions/${id}`)
        .then((data) => {
          if (latestSelection.current === id) setDetail(data);
        })
        .catch((err) => {
          if (handleError(err) || latestSelection.current !== id) return;
          setDetailError(err instanceof ApiError && err.status === 404 ? "Версію не знайдено." : "Не вдалося завантажити версію.");
        });
    },
    [handleError],
  );

  const refreshLists = useCallback(
    () =>
      loadLists()
        .then((data) => {
          setLists(data);
          setListError(null);
          return data;
        })
        .catch((err) => {
          if (!handleError(err)) setListError("Не вдалося завантажити списки версій.");
          return null;
        }),
    [handleError],
  );

  useEffect(() => {
    refreshLists().then((data) => {
      if (!data) return;
      if (initialVersion) {
        selectVersion(initialVersion);
      } else {
        const first = data[isTabKey(initialTab) ? initialTab : "pending"][0];
        selectVersion(first?.versionId ?? null);
      }
    });
    // All deps are stable for the page's lifetime (the URL only seeds the initial tab/selection
    // and is never changed in-page), so this runs once.
  }, [refreshLists, selectVersion, initialVersion, initialTab]);

  function switchTab(next: TabKey) {
    setTab(next);
    setVoicing(null);
    selectVersion(lists?.[next][0]?.versionId ?? null);
  }

  async function act(action: Action) {
    if (!detail) return;
    const versionId = detail.id;
    setActing(action);
    setActionError(null);
    try {
      if (action === "voice") {
        const result = await apiFetch<VoicingResult>(`/admin/lesson-versions/${versionId}/voice`, { method: "POST" });
        setVoicing({ versionId, result });
        setVoicedThisSession((prev) => new Set(prev).add(versionId));
        setPublishWarningOpen(false);
      } else {
        const body = action === "reject" ? JSON.stringify({ reason }) : undefined;
        const updated = await apiFetch<{ status: LessonVersionStatus }>(
          `/admin/lesson-versions/${versionId}/${action}`,
          { method: "POST", body },
        );
        // Keep the version open in its new state (e.g. voice/publish right after approving).
        setDetail((d) => (d && d.id === versionId ? { ...d, status: updated.status } : d));
        setRejectOpen(false);
        setReason("");
        setPublishWarningOpen(false);
      }
      await refreshLists();
    } catch (err) {
      if (!handleError(err)) setActionError(actionErrorText(action, err));
    } finally {
      setActing(null);
    }
  }

  function onPublish() {
    if (detail && !voicedThisSession.has(detail.id) && !publishWarningOpen) {
      setPublishWarningOpen(true);
      return;
    }
    void act("publish");
  }

  const currentList = lists?.[tab] ?? null;
  const detailStatus = detail ? VERSION_STATUS_LABELS[detail.status] : null;
  const detailTab = detail ? moderationTab(detail.status) : null;

  return (
    <>
      <AppHeader context="Модерація контенту" />

      <main className="flex flex-1 flex-col gap-5.5 px-8 pt-7 pb-18">
        <div className="flex flex-wrap items-end justify-between gap-5">
          <div className="flex flex-col gap-2">
            <SecondaryLink href="/admin">← Адмін-кабінет</SecondaryLink>
            <h1 className="text-[30px] leading-[1.15] font-medium tracking-[-0.02em]">Модерація уроків</h1>
            <p className="text-ink-muted text-base">
              ШІ-контент доходить до дітей лише через «Схвалити» → «Опублікувати».
            </p>
          </div>
          <Metric label="Озвучено за сесію" value={String(voicedThisSession.size)} />
        </div>

        <div role="tablist" aria-label="Статус версій" className="flex flex-wrap gap-2">
          {TABS.map((t) => {
            const active = t.key === tab;
            return (
              <button
                key={t.key}
                type="button"
                role="tab"
                aria-selected={active}
                onClick={() => switchTab(t.key)}
                className={`flex h-10 cursor-pointer items-center gap-2 rounded-lg border px-4 text-[15px] font-medium transition-colors duration-100 ${
                  active
                    ? "border-brand bg-brand-tint text-brand"
                    : "border-line-field bg-surface text-avatar-ink hover:bg-canvas"
                }`}
              >
                {t.label}
                <span className="text-ink-icon font-mono text-[13px]">{lists ? lists[t.key].length : "—"}</span>
              </button>
            );
          })}
        </div>

        <div className="grid grid-cols-1 items-start gap-5 xl:grid-cols-[minmax(360px,1fr)_minmax(420px,1.05fr)]">
          <section className="bg-surface border-line shadow-card overflow-hidden rounded-xl border">
            <div className="bg-row-hover text-ink-faint border-line-head grid grid-cols-[1fr_62px_92px] gap-2.5 border-b px-4.5 py-3 font-mono text-xs tracking-[0.08em] uppercase">
              <span>Урок</span>
              <span>Версія</span>
              <span>Автор</span>
            </div>

            {listError ? <p className="text-danger px-4.5 py-4 text-sm">{listError}</p> : null}
            {!currentList && !listError ? <p className="text-ink-muted px-4.5 py-4 text-sm">Завантаження…</p> : null}
            {currentList && currentList.length === 0 ? (
              <p className="text-ink-muted px-4.5 py-5 text-sm">Тут поки порожньо.</p>
            ) : null}

            {currentList?.map((entry) => {
              const selected = entry.versionId === selectedId;
              const entryStatus = VERSION_STATUS_LABELS[entry.status];
              return (
                <button
                  key={entry.versionId}
                  type="button"
                  onClick={() => selectVersion(entry.versionId)}
                  aria-current={selected}
                  className={`border-line-row grid w-full cursor-pointer grid-cols-[1fr_62px_92px] items-center gap-2.5 border-b border-l-3 px-4.5 py-3.5 text-left transition-colors duration-100 ${
                    selected ? "border-l-brand bg-brand-tint" : "bg-surface hover:bg-row-hover border-l-transparent"
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
            {detailError ? <p className="text-danger px-6 py-5 text-sm">{detailError}</p> : null}
            {!detail && !detailError ? (
              <p className="text-ink-muted px-6 py-5 text-sm">
                {selectedId ? "Завантаження сценарію…" : "Виберіть версію зі списку."}
              </p>
            ) : null}

            {detail ? (
              <>
                <div className="border-line-soft flex flex-col gap-2.5 border-b px-6 pt-5 pb-4.5">
                  <div className="flex flex-wrap items-center gap-2.5">
                    <h2 className="text-[21px] font-medium tracking-[-0.01em]">{detail.scenario.title}</h2>
                    {detailStatus ? <Pill tone={detailStatus.tone}>{detailStatus.label}</Pill> : null}
                  </div>
                  <div className="text-ink-icon flex flex-wrap gap-4.5 font-mono text-[13px]">
                    <span>{detail.scenario.module}</span>
                    <span>v{detail.versionNo}</span>
                    <span>{detail.aiModel ?? detail.generatedBy}</span>
                    <span>{new Date(detail.createdAt).toLocaleString("uk-UA")}</span>
                  </div>
                  {detailTab && detailTab !== tab ? (
                    <button
                      type="button"
                      onClick={() => {
                        setTab(detailTab);
                        setVoicing(null);
                      }}
                      className="text-brand self-start cursor-pointer text-sm underline underline-offset-[3px]"
                    >
                      Перейти до вкладки «{TABS.find((t) => t.key === detailTab)?.label}»
                    </button>
                  ) : null}
                </div>

                <div className="flex max-h-115 flex-col gap-4 overflow-y-auto px-6 pt-5 pb-5.5">
                  <span className="text-ink-faint font-mono text-xs tracking-[0.09em] uppercase">Сценарій уроку</span>
                  <div className="flex flex-col gap-1.25">
                    <span className="text-ink-icon text-[13px]">Навчальна мета</span>
                    <p className="text-[15px] leading-relaxed text-pretty">{detail.scenario.learning_goal}</p>
                  </div>
                  {detail.scenario.scenes.map((scene) => (
                    <SceneCard key={scene.key} scene={scene} />
                  ))}
                </div>

                <div className="bg-row-hover border-line-soft flex flex-col gap-3.5 border-t px-6 pt-5 pb-6">
                  {actionError ? <p className="text-danger text-sm">{actionError}</p> : null}

                  {voicing && voicing.versionId === detail.id ? (
                    <div className="bg-ok-surface border-ok-line text-ok-ink rounded-lg border px-3.75 py-3.25 text-sm leading-relaxed">
                      Озвучено: рядків {voicing.result.linesTotal}, синтезовано {voicing.result.synthesized}, з кешу{" "}
                      {voicing.result.cached}.
                    </div>
                  ) : null}

                  {detail.status === "auto_validated" ? (
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

                  {detail.status === "auto_validated" && rejectOpen ? (
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

                  {detail.status === "approved" ? (
                    <>
                      {publishWarningOpen ? (
                        <Notice
                          title="Урок без озвучки буде незручний для дітей. Озвучити спочатку?"
                          action={
                            <>
                              <NoticeButton type="button" onClick={() => act("voice")} disabled={acting !== null}>
                                {acting === "voice" ? "Озвучуємо…" : "Озвучити"}
                              </NoticeButton>
                              <NoticeButton type="button" onClick={onPublish} disabled={acting !== null}>
                                Опублікувати без озвучки
                              </NoticeButton>
                            </>
                          }
                        />
                      ) : null}
                      <div className="flex flex-wrap gap-2.5">
                        <SecondaryButton
                          type="button"
                          onClick={() => act("voice")}
                          disabled={acting !== null}
                          className="h-13 flex-1 justify-center text-[17px] disabled:opacity-50"
                        >
                          {acting === "voice" ? "Озвучуємо…" : "Озвучити"}
                        </SecondaryButton>
                        <Button
                          type="button"
                          onClick={onPublish}
                          disabled={acting !== null}
                          className="bg-mark-ok hover:bg-mark-ok/90 active:bg-mark-ok/80 h-13 flex-1 text-[17px]"
                        >
                          {acting === "publish" ? "Публікуємо…" : "Опублікувати"}
                        </Button>
                      </div>
                    </>
                  ) : null}

                  {detail.status === "published" ? (
                    <SecondaryButton
                      type="button"
                      onClick={() => act("voice")}
                      disabled={acting !== null}
                      className="h-12 justify-center text-[16px] disabled:opacity-50"
                    >
                      {acting === "voice" ? "Озвучуємо…" : "Озвучити (доозвучити нові рядки)"}
                    </SecondaryButton>
                  ) : null}

                  {detail.status === "rejected_auto" || detail.status === "archived" || detail.status === "draft" ? (
                    <p className="text-ink-muted text-sm">Для цієї версії дій немає.</p>
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
