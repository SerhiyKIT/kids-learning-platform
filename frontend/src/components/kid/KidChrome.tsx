"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useKidMode } from "./kid-mode-context";
import { AccessoryMark, useKidTheme, WorldDecor } from "./KidTheme";

/** Тепле тло дитячого режиму з двома м'якими колами. Займає весь екран. */
export function KidStage({ children }: { children: React.ReactNode }) {
  return (
    <div className="font-kid relative flex h-[100dvh] w-screen overflow-hidden">
      <StageBackdrop />
      {children}
    </div>
  );
}

/**
 * The stage's background, drawn only from theme tokens: the gradient, two soft circles and the
 * current world's decor. Also used by the admin's theme preview.
 */
export function StageBackdrop() {
  const { worldId } = useKidTheme();
  return (
    <>
      <span
        aria-hidden="true"
        className="absolute inset-0 bg-[radial-gradient(120%_90%_at_12%_20%,var(--color-kid-bg-top)_0%,var(--color-kid-canvas)_55%,var(--color-kid-bg-bottom)_100%)]"
      />
      <span aria-hidden="true" className="bg-kid-glow absolute -bottom-45 -left-30 size-130 rounded-full opacity-55" />
      <span aria-hidden="true" className="bg-kid-bg-top absolute -top-40 -right-35 size-115 rounded-full opacity-60" />
      <WorldDecor worldId={worldId} />
    </>
  );
}

/** How long an adult holds the exit button before the password screen opens. */
const EXIT_HOLD_MS = 2000;
const EXIT_RING_RADIUS = 21;
const EXIT_RING_LENGTH = 2 * Math.PI * EXIT_RING_RADIUS;

/**
 * Exit from child mode — small, muted and in the corner, and it must be HELD for ~2 s (a ring
 * fills around it); letting go earlier does nothing, so a child's tap never leaves. Holding then
 * opens the adult's password screen (docs/Ролі_та_приватність.md §2). Keyboard: hold Enter/Space.
 */
export function ExitButton() {
  const { requestExit } = useKidMode();
  const [progress, setProgress] = useState(0);
  const frame = useRef<number | null>(null);

  const cancel = useCallback(() => {
    if (frame.current !== null) cancelAnimationFrame(frame.current);
    frame.current = null;
    setProgress(0);
  }, []);

  const begin = useCallback(() => {
    cancel();
    const startedAt = performance.now();
    const tick = (now: number) => {
      const p = Math.min(1, (now - startedAt) / EXIT_HOLD_MS);
      if (p >= 1) {
        frame.current = null;
        setProgress(0);
        requestExit();
        return;
      }
      setProgress(p);
      frame.current = requestAnimationFrame(tick);
    };
    frame.current = requestAnimationFrame(tick);
  }, [cancel, requestExit]);

  useEffect(() => cancel, [cancel]);

  return (
    <button
      type="button"
      aria-label="Вихід для дорослих: утримуйте 2 секунди"
      onPointerDown={(e) => {
        if (e.button !== 0) return;
        e.currentTarget.setPointerCapture(e.pointerId);
        begin();
      }}
      onPointerUp={cancel}
      onPointerCancel={cancel}
      onLostPointerCapture={cancel}
      onKeyDown={(e) => {
        if ((e.key === "Enter" || e.key === " ") && !e.repeat) {
          e.preventDefault();
          begin();
        }
      }}
      onKeyUp={(e) => {
        if (e.key === "Enter" || e.key === " ") cancel();
      }}
      onBlur={cancel}
      onContextMenu={(e) => e.preventDefault()}
      className="text-kid-ink-soft absolute top-4.5 right-4.5 z-10 flex size-11.5 cursor-pointer touch-none items-center justify-center rounded-[14px] border-[1.5px] border-[rgba(126,88,44,0.25)] bg-white/60 select-none [-webkit-touch-callout:none]"
    >
      <svg
        className="pointer-events-none absolute -inset-1 size-[calc(100%+8px)] -rotate-90"
        viewBox="0 0 50 50"
        aria-hidden="true"
        style={{ opacity: progress > 0 ? 1 : 0 }}
      >
        <circle
          cx="25"
          cy="25"
          r={EXIT_RING_RADIUS}
          fill="none"
          stroke="var(--color-kid-ink-soft)"
          strokeWidth="3"
          strokeLinecap="round"
          strokeDasharray={EXIT_RING_LENGTH}
          strokeDashoffset={EXIT_RING_LENGTH * (1 - progress)}
        />
      </svg>
      <svg width="20" height="20" viewBox="0 0 20 20" fill="none" aria-hidden="true">
        <path d="M12 3.5H15.5V16.5H12" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M9.5 13.5 6 10l3.5-3.5M6 10h6" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    </button>
  );
}

/**
 * Маскот-провідник. Справжня ілюстрація ще не намальована — до того часу тут
 * штрихований силует того самого розміру, щоб композиція не «стрибнула» при заміні
 * (див. docs/Специфікація_ассетів.md).
 */
export function Mascot({
  size = "lg",
  speaking = false,
  className,
}: {
  size?: "lg" | "sm";
  speaking?: boolean;
  /** Overrides the fixed lg/sm box, e.g. viewport-relative sizing inside a scene. */
  className?: string;
}) {
  const big = size === "lg";
  const { guideAccessory } = useKidTheme();
  return (
    <div
      className={`relative motion-reduce:animate-none ${speaking ? "animate-[talk_0.45s_ease-in-out_infinite]" : "animate-[bob_4.2s_ease-in-out_infinite]"} ${className ?? (big ? "h-60 w-50" : "h-42 w-35")}`}
      role="img"
      aria-label="Маскот-провідник"
    >
      <span className="absolute inset-0 rounded-[46%_46%_40%_40%] border-5 border-white bg-[repeating-linear-gradient(135deg,var(--color-kid-guide-main)_0_12px,var(--color-kid-guide-accent)_12px_24px)] shadow-[0_16px_0_color-mix(in_srgb,var(--color-kid-teal)_25%,transparent)]" />
      <AccessoryMark accessory={guideAccessory} />
    </div>
  );
}

/** Велика жовта кнопка «послухати». Головна кнопка дитячого режиму — мінімум 80px. */
export function ListenButton({
  onClick,
  size = "lg",
  label = "Послухати",
  speaking = false,
  animation = "pulsedot",
  className,
}: {
  onClick?: () => void;
  size?: "lg" | "sm";
  label?: string;
  /** While the line is being heard the button "talks" instead of pulsing. */
  speaking?: boolean;
  /** Idle animation: a gentle pulse, or a slower "breathing" one for the lesson start. */
  animation?: "pulsedot" | "breathe";
  /** Overrides the fixed lg/sm size, e.g. viewport-relative sizing inside a scene (keep ≥ 64px). */
  className?: string;
}) {
  const big = size === "lg";
  const idle = animation === "breathe" ? "animate-[breathe_2.4s_ease-in-out_infinite]" : "animate-[pulsedot_2.6s_ease-in-out_infinite]";
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={label}
      className={`bg-kid-sun text-kid-sun-deep shadow-kid-sun-shadow flex cursor-pointer items-center justify-center rounded-full border-6 border-white shadow-[0_12px_0_var(--color-kid-sun-shadow)] motion-reduce:animate-none ${speaking ? "animate-[talk_0.45s_ease-in-out_infinite]" : idle} ${
        className ?? (big ? "size-27" : "size-20")
      }`}
    >
      <svg width={big ? 46 : 40} height={big ? 46 : 40} viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M4 9.5h3.2L12 5.5v13l-4.8-4H4v-5Z" fill="currentColor" />
        <path d="M15.5 9c1.2 1.1 1.2 4.9 0 6M18.4 6.6c2.2 2.2 2.2 8.6 0 10.8" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" />
      </svg>
    </button>
  );
}

/** Галочка-печатка «пройдено». */
export function DoneStamp({ className = "" }: { className?: string }) {
  return (
    <span
      aria-hidden="true"
      className={`bg-kid-teal flex size-[clamp(52px,14dvh,92px)] -rotate-12 items-center justify-center rounded-full border-[clamp(4px,1dvh,6px)] border-white shadow-[0_8px_0_var(--color-kid-teal-shadow)] ${className}`}
    >
      <svg className="size-[48%]" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M5.5 12.8l4.2 4.2L18.5 8" stroke="#ffffff" strokeWidth="3.4" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    </span>
  );
}

/** Смужка-прогрес зі сцен: пройдені — бірюзові, поточна — жовта і більша. */
export function StepDots({ total, current }: { total: number; current: number }) {
  return (
    <div className="relative z-2 flex flex-none items-center justify-center gap-[clamp(8px,2dvh,18px)] px-21 pt-[clamp(4px,2dvh,20px)] pb-1">
      {Array.from({ length: total }, (_, i) => {
        const done = i < current;
        const isCurrent = i === current;
        return (
          <span
            key={i}
            className={`rounded-full border-4 border-white shadow-[0_4px_0_var(--color-kid-card-shadow)] ${
              isCurrent
                ? "bg-kid-sun size-[clamp(18px,5dvh,34px)] animate-[pulsedot_2.2s_ease-in-out_infinite] motion-reduce:animate-none"
                : done
                  ? "bg-kid-teal size-[clamp(14px,4dvh,26px)]"
                  : "bg-kid-pip size-[clamp(14px,4dvh,26px)]"
            }`}
          />
        );
      })}
    </div>
  );
}

/**
 * Заглушка ілюстрації сцени/уроку. Той самий контракт, що й SceneAsset у scene-engine:
 * єдине місце, яке знає, що справжніх картинок ще немає.
 */
const KID_STRIPES = [
  ["#ffd7c2", "#ffc7ac"],
  ["#d8dcf7", "#c9ceef"],
  ["#ffe3a6", "#ffd782"],
  ["#cfe9e2", "#bfe1d8"],
  ["#f0dcef", "#e6cde5"],
  ["#dde7c9", "#cfdcb5"],
];

export function stripeFor(seed: string): string {
  let hash = 0;
  for (let i = 0; i < seed.length; i += 1) hash = (hash * 31 + seed.charCodeAt(i)) % 9973;
  const [a, b] = KID_STRIPES[hash % KID_STRIPES.length];
  return `repeating-linear-gradient(135deg, ${a} 0 14px, ${b} 14px 28px)`;
}
