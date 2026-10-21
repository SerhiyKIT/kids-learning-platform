"use client";

import type { ReactNode } from "react";
import type { VoiceLine } from "@/lib/scenario-types";
import { ExitButton, KidStage, ListenButton, Mascot, StepDots, stripeFor } from "@/components/kid/KidChrome";
import { useAudioBlocked, useSpeakingUrl } from "./KidAudio";

/**
 * Shared frame of every kid scene, landscape-first (docs/UX_гайд_дитячого_режиму.md §2):
 * - sized off the dynamic viewport (dvh) and clamp() so it holds from a 740×360 phone to a desktop
 *   with nothing clipped and no scrolling;
 * - a 16–24px dead zone around the edge that holds no interactive element;
 * - ONE guide (mascot + one «послухати» button): tapping either is the guide tap; while a line is
 *   heard both "talk", and the button pulses harder when the browser blocked the sound.
 *
 * Rows: [stage | guide] takes whatever height is left, then the caption (adult's dub of the voice),
 * then the scene's interactive area at its natural size.
 */
export function SceneShell({
  background,
  stage,
  caption,
  hint,
  onGuideTap,
  progress,
  children,
}: {
  background: string;
  /** Replaces the illustration (e.g. sorting baskets). */
  stage?: ReactNode;
  caption: ReactNode;
  /** Current hint line: shown small, as text for the adult (the child hears it). */
  hint?: VoiceLine;
  onGuideTap: () => void;
  progress?: { index: number; total: number };
  children: ReactNode;
}) {
  const speaking = useSpeakingUrl() !== null;
  const blocked = useAudioBlocked();

  return (
    <KidStage>
      <ExitButton />
      {/* Dead zone: ≥ 20px of nothing tappable along every edge — a hand holding the device, plus
          room for the cards' "breathing" scale and drop shadow. */}
      <div className="relative z-2 flex h-full min-h-0 w-full flex-col px-[clamp(20px,3vw,28px)] pt-[clamp(8px,2dvh,20px)] pb-[clamp(22px,4dvh,30px)]">
        {progress ? <StepDots total={progress.total} current={progress.index} /> : null}

        <div className="flex min-h-0 flex-1 items-stretch gap-[clamp(10px,2.5vw,26px)] pt-[clamp(4px,1.5dvh,10px)] pr-[clamp(48px,6vw,64px)]">
          {stage ?? (
            <div
              className="min-h-0 min-w-0 flex-1 rounded-[clamp(18px,5dvh,40px)] border-[clamp(4px,1dvh,7px)] border-white shadow-[0_10px_0_var(--color-kid-card-shadow)]"
              style={{ background: stripeFor(background) }}
              role="img"
              aria-label={`Ілюстрація сцени: ${background}`}
            />
          )}
          <div className="flex flex-none flex-col items-center justify-center gap-[clamp(6px,2dvh,14px)] [@media(max-height:500px)]:flex-row">
            <button type="button" onClick={onGuideTap} aria-label="Провідник: пояснити ще раз" className="cursor-pointer">
              <Mascot speaking={speaking} className="h-[clamp(64px,24dvh,168px)] w-[clamp(54px,20dvh,140px)]" />
            </button>
            <ListenButton
              label="Послухати ще раз"
              onClick={onGuideTap}
              speaking={speaking}
              className={`size-[clamp(68px,14dvh,96px)] ${blocked ? "ring-8 ring-white/80" : ""}`}
            />
          </div>
        </div>

        <div className="flex flex-none flex-col items-center gap-1 px-[clamp(8px,4vw,40px)] pt-[clamp(6px,2dvh,14px)]">
          {caption}
          {hint ? (
            <p className="text-kid-sun-deep rounded-2xl border-2 border-white bg-kid-bg-top px-3 py-1 text-center text-[clamp(13px,3.6dvh,20px)] font-semibold">
              {hint.text}
            </p>
          ) : null}
        </div>

        <div className="flex flex-none justify-center pt-[clamp(6px,2dvh,14px)]">{children}</div>
      </div>
    </KidStage>
  );
}

/** Big round "next" / "try again" button after feedback (≥ 64px, breathing while it waits). */
export function NextButton({
  kind,
  disabled,
  onClick,
}: {
  kind: "next" | "retry";
  disabled?: boolean;
  onClick: () => void;
}) {
  const next = kind === "next";
  return (
    <button
      type="button"
      disabled={disabled}
      onClick={onClick}
      aria-label={next ? "Далі" : "Спробувати ще"}
      className={`flex size-[clamp(64px,18dvh,104px)] cursor-pointer items-center justify-center rounded-full border-[clamp(4px,1dvh,7px)] border-white transition-opacity duration-200 disabled:opacity-40 ${
        next
          ? "bg-kid-teal text-white shadow-[0_10px_0_var(--color-kid-teal-shadow)] animate-[breathe_2.2s_ease-in-out_infinite] motion-reduce:animate-none"
          : "bg-kid-sun text-kid-sun-deep shadow-[0_10px_0_var(--color-kid-sun-shadow)]"
      }`}
    >
      {next ? (
        <svg className="size-1/2" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <path d="M6 12h11M12.5 6.5 18 12l-5.5 5.5" stroke="currentColor" strokeWidth="2.8" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      ) : (
        <svg className="size-1/2" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <path d="M19 12a7 7 0 1 1-2.05-4.95M19 4.5V8h-3.5" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      )}
    </button>
  );
}
