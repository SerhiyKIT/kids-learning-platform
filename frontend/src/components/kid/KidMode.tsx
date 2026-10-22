"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useRef, useState, useSyncExternalStore, type ReactNode } from "react";
import { apiFetch, ApiError } from "@/lib/api";
import type { ShellLine } from "@/lib/api-types";
import type { StyleResponse } from "@/lib/theme-catalog";
import { useKidAudio } from "@/components/scene-engine/KidAudio";
import { Button } from "@/components/ui/Button";
import { PasswordInput } from "@/components/ui/Field";
import { QuietButton } from "@/components/ui/SecondaryButton";
import { ExitButton, KidStage, ListenButton, Mascot } from "./KidChrome";
import { KidModeContext, type KidMode, type ShellLineKey } from "./kid-mode-context";
import { KidThemeScope } from "./KidTheme";
import { useOutbox } from "./outbox";

/**
 * The child-mode shell around the shelf, the album and every lesson (one per child, kept across
 * client-side navigation between them, together with the one unlocked audio element):
 * - «Почнімо!» first: the tap that unlocks sound and tries fullscreen + a landscape lock;
 * - a full-screen "turn the device" picture (voiced once) whenever the device is in portrait;
 * - the protected exit: the held ExitButton opens the adult's password screen, with child-mode
 *   sound suspended while it's open;
 * - the child's approved theme (palette, world decor, guide accessory) over all of it.
 * Must sit inside a KidAudioProvider.
 */
export function KidModeProvider({ childId, children }: { childId: string; children: ReactNode }) {
  const router = useRouter();
  const { unlock, play, preload, suspend } = useKidAudio();
  const [started, setStarted] = useState(false);
  const [lines, setLines] = useState<Map<string, ShellLine>>(new Map());
  const [exitOpen, setExitOpen] = useState(false);
  const [style, setStyle] = useState<StyleResponse | null>(null);
  const [styleReady, setStyleReady] = useState(false);
  const portrait = usePortrait();
  const turnVoiced = useRef(false);
  const outbox = useOutbox();
  const sendResult = useCallback(<T,>(path: string, body: unknown) => outbox.send<T>(path, body), [outbox]);

  useEffect(() => {
    apiFetch<ShellLine[]>("/kid/shell-lines")
      .then((loaded) => {
        setLines(new Map(loaded.map((l) => [l.key, l])));
        preload(loaded.flatMap((l) => (l.audioUrl ? [l.audioUrl] : [])));
      })
      // Without the lines the guide is simply quiet around lessons — never an error for the child.
      .catch(() => {});
  }, [preload]);

  useEffect(() => {
    apiFetch<StyleResponse>(`/children/${childId}/style`)
      .then(setStyle)
      // No style → the default theme from globals.css; never an error for the child.
      .catch(() => {})
      .finally(() => setStyleReady(true));
  }, [childId]);

  const line = useCallback((key: ShellLineKey) => lines.get(key), [lines]);

  // Once per visit, the first time the child is in portrait after sound is unlocked.
  useEffect(() => {
    const url = lines.get("turn_device")?.audioUrl;
    if (started && portrait && url && !turnVoiced.current) {
      turnVoiced.current = true;
      void play(url);
    }
  }, [started, portrait, lines, play]);

  const requestExit = useCallback(() => {
    suspend(true);
    setExitOpen(true);
  }, [suspend]);

  const cancelExit = useCallback(() => {
    setExitOpen(false);
    suspend(false);
  }, [suspend]);

  const exited = useCallback(() => {
    leaveLandscape();
    router.push(`/children/${childId}`);
  }, [router, childId]);

  function start() {
    unlock(); // synchronously inside the tap: a silent clip unlocks the shared audio element
    void enterLandscape();
    setStarted(true);
  }

  const value = useMemo<KidMode>(
    () => ({ childId, line, requestExit, style, styleReady, setStyle, sendResult }),
    [childId, line, requestExit, style, styleReady, sendResult],
  );

  return (
    <KidModeContext.Provider value={value}>
      <KidThemeScope theme={style?.theme ?? null}>
        {started ? children : <StartScreen onStart={start} />}
        {portrait ? <TurnDeviceOverlay onStart={started ? undefined : start} /> : null}
      </KidThemeScope>
      {exitOpen ? (
        <ExitPasswordDialog onCancel={cancelExit} onExited={exited} onSignedOut={() => router.push("/login")} />
      ) : null}
    </KidModeContext.Provider>
  );
}

/** «Почнімо!» — one tap before anything else: browsers only allow sound after a user gesture. */
function StartScreen({ onStart }: { onStart: () => void }) {
  return (
    <KidStage>
      <ExitButton />
      <div className="relative z-2 flex h-full w-full flex-col items-center justify-center gap-8 landscape:flex-row landscape:gap-16">
        <button type="button" onClick={onStart} aria-label="Почнімо!" className="cursor-pointer">
          <Mascot className="h-[clamp(120px,46dvh,240px)] w-[clamp(100px,38dvh,200px)]" />
        </button>
        <ListenButton label="Почнімо!" onClick={onStart} animation="breathe" className="size-[clamp(80px,22dvh,108px)]" />
      </div>
    </KidStage>
  );
}

type LockableOrientation = ScreenOrientation & {
  lock?: (orientation: "landscape") => Promise<void>;
  unlock?: () => void;
};

/** Best effort: iOS supports neither, and desktop browsers refuse the lock — the overlay covers that. */
async function enterLandscape() {
  try {
    if (!document.fullscreenElement) await document.documentElement.requestFullscreen({ navigationUI: "hide" });
  } catch {
    // Not supported or refused.
  }
  try {
    await (screen.orientation as LockableOrientation).lock?.("landscape");
  } catch {
    // Not supported, or not in fullscreen.
  }
}

function leaveLandscape() {
  try {
    (screen.orientation as LockableOrientation).unlock?.();
  } catch {
    // Not supported.
  }
  if (document.fullscreenElement) void document.exitFullscreen().catch(() => {});
}

const PORTRAIT_QUERY = "(orientation: portrait)";

function usePortrait(): boolean {
  return useSyncExternalStore(
    (onChange) => {
      const query = window.matchMedia(PORTRAIT_QUERY);
      query.addEventListener("change", onChange);
      return () => query.removeEventListener("change", onChange);
    },
    () => window.matchMedia(PORTRAIT_QUERY).matches,
    () => false,
  );
}

/**
 * A picture, not text: a phone turning on its side. Before «Почнімо!» it carries that button too:
 * the tap is what asks for fullscreen + a landscape lock (Android), and a phone whose rotation is
 * locked in portrait would otherwise never get past this picture. The one line of text is for the
 * adult, for the devices where nothing can turn the screen but its owner (iOS rotation lock).
 */
function TurnDeviceOverlay({ onStart }: { onStart?: () => void }) {
  return (
    <div
      className="fixed inset-0 z-40 flex flex-col items-center justify-center gap-6 bg-[radial-gradient(120%_90%_at_50%_30%,var(--color-kid-bg-top)_0%,var(--color-kid-canvas)_60%,var(--color-kid-bg-bottom)_100%)]"
    >
      {/* The adult can still leave from here. */}
      <ExitButton />
      <svg viewBox="0 0 200 200" className="size-[min(70vw,60dvh)]" role="img" aria-label="Поверни пристрій горизонтально">
        <path
          d="M52 44a62 62 0 0 1 96 6"
          fill="none"
          stroke="var(--color-kid-teal)"
          strokeWidth="9"
          strokeLinecap="round"
        />
        <path d="M150 30l1 24-23-4" fill="none" stroke="var(--color-kid-teal)" strokeWidth="9" strokeLinecap="round" strokeLinejoin="round" />
        <g className="origin-center animate-[turnphone_2.8s_ease-in-out_infinite] motion-reduce:rotate-90 motion-reduce:animate-none">
          <rect x="68" y="56" width="64" height="112" rx="14" fill="#ffffff" stroke="var(--color-kid-ink)" strokeWidth="7" />
          <rect x="78" y="70" width="44" height="78" rx="6" fill="var(--color-kid-sun)" />
          <circle cx="100" cy="158" r="4" fill="var(--color-kid-ink)" />
        </g>
      </svg>
      {onStart ? <ListenButton label="Почнімо!" onClick={onStart} animation="breathe" className="size-[clamp(80px,22dvh,108px)]" /> : null}
      <p className="text-kid-ink-soft max-w-[80vw] text-center text-sm">
        Для дорослого: якщо екран не повертається, вимкніть блокування повороту.
      </p>
    </div>
  );
}

/**
 * The adult's side of the protected exit. Plain adult UI with text (the child can't read it, and
 * isn't meant to use it). The password only ever goes to POST /api/auth/reauthenticate and is
 * dropped with the dialog.
 */
function ExitPasswordDialog({
  onCancel,
  onExited,
  onSignedOut,
}: {
  onCancel: () => void;
  onExited: () => void;
  /** The session itself has expired: the adult has to sign in again anyway. */
  onSignedOut: () => void;
}) {
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<ApiError | null>(null);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onCancel();
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [onCancel]);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await apiFetch("/auth/reauthenticate", { method: "POST", body: JSON.stringify({ password }) });
      onExited();
    } catch (err) {
      // A wrong password is a 401 with BAD_CREDENTIALS; a bare 401 means there's no session left.
      if (err instanceof ApiError && err.status === 401 && err.code !== "BAD_CREDENTIALS") {
        onSignedOut();
        return;
      }
      setError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка"));
      setPassword("");
      setSubmitting(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-[color:var(--color-ink)]/55 p-4">
      <form
        role="dialog"
        aria-modal="true"
        aria-labelledby="kid-exit-title"
        onSubmit={onSubmit}
        className="bg-surface border-line flex w-full max-w-[420px] flex-col gap-4 rounded-xl border p-6 shadow-[0_18px_44px_rgba(18,40,61,0.22)]"
      >
        <h2 id="kid-exit-title" className="text-[20px] font-medium tracking-[-0.01em]">
          Вихід з дитячого режиму
        </h2>
        <div className="flex flex-col gap-[7px]">
          <label htmlFor="kid-exit-password" className="text-[15px] font-medium">
            Введіть пароль, щоб вийти з дитячого режиму
          </label>
          <PasswordInput
            id="kid-exit-password"
            required
            autoFocus
            autoComplete="current-password"
            placeholder="Пароль дорослого"
            invalid={error?.status === 401}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          {error ? (
            <p role="alert" className="text-danger text-sm">
              {exitErrorText(error)}
            </p>
          ) : null}
        </div>
        <div className="flex justify-end gap-2.5">
          <QuietButton type="button" className="h-11 px-4 text-[15px]" onClick={onCancel}>
            Скасувати
          </QuietButton>
          <Button type="submit" disabled={submitting || password === ""} className="h-11 w-auto px-5 text-[15px]">
            {submitting ? "Перевіряємо…" : "Вийти"}
          </Button>
        </div>
      </form>
    </div>
  );
}

/** Same wording as the login page for the rate limit. */
function exitErrorText(error: ApiError): string {
  if (error.status === 401) return "Невірний пароль.";
  if (error.status === 429) {
    return error.retryAfterSeconds
      ? `Занадто багато спроб. Спробуйте за ${error.retryAfterSeconds} с.`
      : "Занадто багато спроб. Спробуйте пізніше.";
  }
  return "Не вдалося перевірити пароль. Спробуйте ще раз.";
}
