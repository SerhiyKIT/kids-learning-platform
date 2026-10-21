"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { apiFetch, ApiError } from "@/lib/api";
import {
  ACCESSORY_LABELS,
  COLOR_LABELS,
  COLOR_SPOTS,
  MOOD_LABELS,
  PALETTE_TOKENS,
  WORLD_LABELS,
  type ColorId,
  type GuideAccessory,
  type MoodId,
  type WorldId,
} from "@/lib/theme-catalog";
import { adminRedirect } from "@/lib/two-factor";
import { AppHeader } from "@/components/ui/AppHeader";
import { Button } from "@/components/ui/Button";
import { Pill } from "@/components/ui/Pill";
import { QuietButton, SecondaryButton } from "@/components/ui/SecondaryButton";
import { ThemePreview } from "@/components/kid/ThemePreview";

type Status = "draft" | "approved" | "rejected";

/** GET /api/admin/themes — mirrors ua.kidlearn.themes.AdminThemeEntry. */
interface AdminTheme {
  id: string;
  name: string;
  preferenceKey: string;
  colorId: ColorId;
  worldId: WorldId;
  moodId: MoodId;
  palette: Record<string, string>;
  guideAccessory: GuideAccessory;
  status: Status;
  rejectReason: string | null;
  generatedBy: string;
  aiModel: string | null;
  createdAt: string;
  approvedAt: string | null;
  contrast: { foreground: string; background: string; ratio: number; minimum: number; passes: boolean }[];
  problems: string[];
}

const TABS: { key: Status; label: string }[] = [
  { key: "draft", label: "На модерації" },
  { key: "approved", label: "Схвалені" },
  { key: "rejected", label: "Відхилені" },
];

/**
 * Theme moderation: every AI-picked theme is seen here as a real child screen before any child can
 * get it. Drafts have already passed the automatic checks (contrast, tokens, no acid colours);
 * themes that failed them land straight in «Відхилені» with the reasons, and can be regenerated.
 */
export default function AdminThemesPage() {
  const router = useRouter();
  const [tab, setTab] = useState<Status>("draft");
  const [lists, setLists] = useState<Record<Status, AdminTheme[]> | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    Promise.all(TABS.map((t) => apiFetch<AdminTheme[]>(`/admin/themes?status=${t.key}`)))
      .then(([draft, approved, rejected]) => {
        setLists({ draft, approved, rejected });
        setError(null);
      })
      .catch((err) => {
        const target = adminRedirect(err);
        if (target) {
          router.replace(target);
          return;
        }
        setError("Не вдалося завантажити теми.");
      });
  }, [router]);

  useEffect(() => {
    load();
  }, [load]);

  const current = lists?.[tab] ?? null;

  return (
    <>
      <AppHeader context="Адміністратор" />
      <main className="flex flex-1 justify-center px-6 pt-10 pb-24">
        <div className="flex w-full max-w-260 flex-col gap-6">
          <div className="flex flex-col gap-2">
            <h1 className="text-[32px] leading-[1.15] font-medium tracking-[-0.02em]">Теми оформлення</h1>
            <p className="text-ink-muted text-base leading-relaxed">
              ШІ лише підбирає кольори з токенів бібліотеки для комбінації «колір · світ · настрій». Дитина
              бачить тему тільки після схвалення — і тоді її отримують усі діти з такими самими відповідями.
            </p>
          </div>

          <div role="tablist" aria-label="Статус тем" className="flex flex-wrap gap-2">
            {TABS.map((t) => {
              const active = t.key === tab;
              return (
                <button
                  key={t.key}
                  type="button"
                  role="tab"
                  aria-selected={active}
                  onClick={() => setTab(t.key)}
                  className={`flex h-10 cursor-pointer items-center gap-2 rounded-lg border px-4 text-[15px] font-medium transition-colors duration-100 ${
                    active ? "border-brand bg-brand-tint text-brand" : "border-line-field bg-surface text-avatar-ink hover:bg-canvas"
                  }`}
                >
                  {t.label}
                  <span className="text-ink-icon font-mono text-[13px]">{lists ? lists[t.key].length : "—"}</span>
                </button>
              );
            })}
          </div>

          {error ? <p className="text-danger text-sm">{error}</p> : null}
          {!current && !error ? <p className="text-ink-muted text-sm">Завантаження…</p> : null}
          {current && current.length === 0 ? <p className="text-ink-muted text-sm">Тут поки порожньо.</p> : null}

          <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
            {current?.map((theme) => <ThemeCard key={theme.id} theme={theme} onChanged={load} />)}
          </div>
        </div>
      </main>
    </>
  );
}

function ThemeCard({ theme, onChanged }: { theme: AdminTheme; onChanged: () => void }) {
  const router = useRouter();
  const [acting, setActing] = useState(false);
  const [rejecting, setRejecting] = useState(false);
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string | null>(null);

  async function act(path: string, body?: unknown) {
    setActing(true);
    setError(null);
    try {
      await apiFetch(path, { method: "POST", body: body === undefined ? undefined : JSON.stringify(body) });
      onChanged();
    } catch (err) {
      const target = adminRedirect(err);
      if (target) {
        router.replace(target);
        return;
      }
      setError(err instanceof ApiError && err.status === 409 ? "Стан теми вже змінився. Оновіть сторінку." : "Не вдалося виконати дію.");
      setActing(false);
    }
  }

  const complete = PALETTE_TOKENS.every((t) => typeof theme.palette[t] === "string");

  return (
    <article data-theme-card={theme.preferenceKey} className="bg-surface border-line shadow-card flex flex-col gap-4 rounded-xl border p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex flex-col gap-1">
          <h2 className="text-[17px] font-medium">{theme.name}</h2>
          <p className="text-ink-muted flex flex-wrap items-center gap-x-3 gap-y-1 text-sm">
            <span className="flex items-center gap-1.5">
              <span aria-hidden="true" className="size-3.5 rounded-full border border-black/10" style={{ background: COLOR_SPOTS[theme.colorId] }} />
              {COLOR_LABELS[theme.colorId]}
            </span>
            <span>{WORLD_LABELS[theme.worldId]}</span>
            <span>{MOOD_LABELS[theme.moodId]}</span>
            <span>Провідник: {ACCESSORY_LABELS[theme.guideAccessory] ?? theme.guideAccessory}</span>
          </p>
          <p className="text-ink-faint font-mono text-xs">
            {theme.preferenceKey} · {theme.aiModel ?? "—"} · {new Date(theme.createdAt).toLocaleString("uk-UA")}
          </p>
        </div>
        <Pill tone={theme.status === "approved" ? "ok" : theme.status === "draft" ? "waiting" : "bad"}>
          {theme.status === "approved" ? "Схвалено" : theme.status === "draft" ? "На модерації" : "Відхилено"}
        </Pill>
      </div>

      {complete ? (
        <ThemePreview theme={{ id: theme.id, palette: theme.palette, worldId: theme.worldId, guideAccessory: theme.guideAccessory }} />
      ) : (
        <p className="text-ink-muted text-sm">Палітри немає — генерація не вдалася.</p>
      )}

      {theme.contrast.length > 0 ? (
        <table className="text-sm">
          <caption className="text-ink-faint pb-1.5 text-left font-mono text-xs tracking-[0.08em] uppercase">Контраст (WCAG)</caption>
          <tbody>
            {theme.contrast.map((c) => (
              <tr key={`${c.foreground}-${c.background}`} className="border-line-row border-b last:border-b-0">
                <td className="py-1 pr-3">
                  <span className="inline-flex items-center gap-1.5">
                    <span aria-hidden="true" className="size-3 rounded-sm border border-black/10" style={{ background: theme.palette[c.foreground] }} />
                    {c.foreground} на {c.background}
                  </span>
                </td>
                <td className="py-1 pr-3 text-right font-mono">{c.ratio.toFixed(2)}:1</td>
                <td className="text-ink-icon py-1 pr-3 text-right font-mono">≥ {c.minimum}</td>
                <td className={`py-1 text-right ${c.passes ? "text-mark-ok" : "text-danger"}`}>{c.passes ? "✓" : "✗"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : null}

      {theme.rejectReason ? (
        <p className="text-danger text-sm">
          <span className="font-medium">Причина: </span>
          {theme.rejectReason}
        </p>
      ) : null}

      {theme.status === "draft" ? (
        rejecting ? (
          <div className="flex flex-col gap-2">
            <label htmlFor={`reason-${theme.id}`} className="text-[15px] font-medium">
              Причина відхилення
            </label>
            <textarea
              id={`reason-${theme.id}`}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              rows={2}
              className="border-line-field rounded-lg border px-3 py-2 text-[15px]"
            />
            <div className="flex justify-end gap-2.5">
              <QuietButton type="button" className="h-10 px-4" onClick={() => setRejecting(false)}>
                Скасувати
              </QuietButton>
              <SecondaryButton
                type="button"
                className="h-10"
                disabled={acting || reason.trim() === ""}
                onClick={() => void act(`/admin/themes/${theme.id}/reject`, { reason: reason.trim() })}
              >
                Відхилити
              </SecondaryButton>
            </div>
          </div>
        ) : (
          <div className="flex justify-end gap-2.5">
            <QuietButton type="button" className="h-10 px-4" onClick={() => setRejecting(true)} disabled={acting}>
              Відхилити…
            </QuietButton>
            <Button type="button" className="h-10 w-auto px-5 text-[15px]" disabled={acting} onClick={() => void act(`/admin/themes/${theme.id}/approve`)}>
              Схвалити
            </Button>
          </div>
        )
      ) : null}

      {theme.status === "rejected" ? (
        <div className="flex justify-end">
          <SecondaryButton
            type="button"
            className="h-10"
            disabled={acting}
            onClick={() => void act("/admin/themes/regenerate", { preferenceKey: theme.preferenceKey })}
          >
            {acting ? "Генеруємо…" : "Перегенерувати"}
          </SecondaryButton>
        </div>
      ) : null}

      {error ? <p className="text-danger text-sm">{error}</p> : null}
    </article>
  );
}
