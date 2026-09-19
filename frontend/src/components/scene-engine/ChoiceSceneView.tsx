"use client";

import { useEffect, useRef, useState } from "react";
import type { ChoiceOption, ChoiceScene } from "@/lib/scenario-types";
import { ExitButton, KidStage, ListenButton, Mascot, StepDots, stripeFor } from "@/components/kid/KidChrome";
import type { AnswerEvent } from "./SceneRenderer";
import { VoiceLinePlayer } from "./VoiceLinePlayer";

type Phase = "choosing" | "feedback";

/**
 * Choice/control scene: setup line + large tappable option cards. Wrong answers are never a
 * dead end (docs/UX_гайд_дитячого_режиму.md §1/§8): feedback plays, the next assistant hint
 * appears (skipped on control scenes — the engine turns hints off there, per the schema), and
 * the child tries again. After both hint levels are used up, the correct card is gently
 * highlighted instead of a third silent hint.
 *
 * sceneIndex/sceneTotal лише малюють смужку прогресу — якщо SceneRenderer їх не передає,
 * смужка не показується, логіка сцени не залежить від них.
 */
export function ChoiceSceneView({
  scene,
  onAdvance,
  onAnswer,
  sceneIndex,
  sceneTotal,
}: {
  scene: ChoiceScene;
  onAdvance: () => void;
  onAnswer: (event: AnswerEvent) => void;
  sceneIndex?: number;
  sceneTotal?: number;
}) {
  const [tryNo, setTryNo] = useState(1);
  const [hintLevel, setHintLevel] = useState(0);
  const [revealCorrect, setRevealCorrect] = useState(false);
  const [phase, setPhase] = useState<Phase>("choosing");
  const [pickedOption, setPickedOption] = useState<ChoiceOption | null>(null);
  const [feedbackReady, setFeedbackReady] = useState(false);
  // Хибний вибір «відскакує» карткою замість того, щоб блокувати екран.
  const [nudged, setNudged] = useState<{ id: string; n: number } | null>(null);
  const nudgeTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => () => {
    if (nudgeTimer.current) clearTimeout(nudgeTimer.current);
  }, []);

  const hint = !scene.is_control && hintLevel > 0 ? scene.assistant.hints.find((h) => h.level === hintLevel) : undefined;

  function choose(option: ChoiceOption) {
    if (phase !== "choosing") return;
    if (!option.correct) {
      setNudged((prev) => ({ id: option.id, n: (prev?.n ?? 0) + 1 }));
      if (nudgeTimer.current) clearTimeout(nudgeTimer.current);
      nudgeTimer.current = setTimeout(() => setNudged(null), 700);
    }
    setPickedOption(option);
    setFeedbackReady(false);
    setPhase("feedback");
    onAnswer({
      sceneKey: scene.key,
      tryNo,
      chosenOption: option.id,
      isCorrect: option.correct,
      hintsUsed: hintLevel,
    });
  }

  function afterFeedback() {
    if (!pickedOption) return;
    if (pickedOption.correct) {
      onAdvance();
      return;
    }
    setTryNo((n) => n + 1);
    if (!scene.is_control) {
      if (hintLevel < 2) {
        setHintLevel((level) => level + 1);
      } else {
        setRevealCorrect(true);
      }
    }
    setPhase("choosing");
    setPickedOption(null);
  }

  const solved = phase === "feedback" && pickedOption?.correct === true;

  return (
    <KidStage>
      <ExitButton />

      <div className="relative z-2 flex h-full min-h-0 w-full flex-col">
        {sceneTotal && sceneIndex != null ? (
          <StepDots total={sceneTotal} current={sceneIndex} />
        ) : null}

        <div className="flex min-h-0 flex-1 items-center gap-6.5 px-10 pt-1.5">
          <div
            className="flex h-[78%] min-w-0 flex-1 items-center justify-center rounded-[40px] border-7 border-white shadow-[0_12px_0_var(--color-kid-card-shadow)]"
            style={{ background: stripeFor(scene.background) }}
            role="img"
            aria-label={`Ілюстрація сцени: ${scene.background}`}
          />
          <div className="flex w-39 flex-none flex-col items-center gap-3.5">
            <Mascot size="sm" />
            <ListenButton size="sm" label="Послухати ще раз" />
          </div>
        </div>

        {/* Озвучення веде сцену; текст лишається видимим для дорослого поруч. */}
        <div className="text-kid-ink px-10 pt-3 text-center text-[26px] leading-tight font-semibold">
          <VoiceLinePlayer key={scene.setup.key} line={scene.setup} />
        </div>

        {hint && phase === "choosing" ? (
          <p className="text-kid-sun-deep mx-10 mt-3 rounded-3xl border-4 border-white bg-[#fff0cd] px-5 py-3 text-center text-[22px] font-semibold">
            {hint.line.text}
          </p>
        ) : null}

        {phase === "choosing" ? (
          <div className="relative z-3 flex flex-none items-end justify-center gap-5 px-10 pt-2.5 pb-6.5">
            {scene.options.map((option) => {
              const isNudged = nudged?.id === option.id;
              const revealed = revealCorrect && option.correct;
              return (
                <div key={option.id} className="relative max-w-75 min-w-44 flex-1">
                  {revealed ? (
                    <span
                      aria-hidden="true"
                      className="bg-kid-correct absolute -inset-4 animate-[glowring_1.6s_ease-in-out_infinite] rounded-[48px]"
                    />
                  ) : null}
                  <button
                    type="button"
                    onClick={() => choose(option)}
                    className={`relative flex w-full cursor-pointer flex-col items-center gap-2.5 rounded-[40px] border-7 px-3.5 pt-3.5 pb-4 shadow-[0_12px_0_var(--color-kid-card-shadow)] ${
                      revealed ? "border-kid-correct bg-kid-correct-surface" : "border-white bg-white"
                    } ${
                      isNudged
                        ? `animate-[softbounce_620ms_ease-out_${nudged.n}]`
                        : "animate-[breathe_2.6s_ease-in-out_infinite]"
                    }`}
                  >
                    <span
                      className="flex h-26.5 w-full items-center justify-center rounded-3xl"
                      style={{ background: stripeFor(option.icon) }}
                      role="img"
                      aria-label={`Малюнок: ${option.icon}`}
                    />
                    <span className="text-kid-ink text-center text-[23px] leading-tight font-semibold">
                      {option.label.text}
                    </span>
                  </button>
                </div>
              );
            })}
          </div>
        ) : pickedOption ? (
          <div className="relative z-3 flex flex-none flex-col items-center gap-4 px-10 pt-2.5 pb-6.5">
            <div className="text-kid-ink text-center text-[24px] leading-tight font-semibold">
              <VoiceLinePlayer
                key={pickedOption.feedback.line.key}
                line={pickedOption.feedback.line}
                onReady={() => setFeedbackReady(true)}
              />
            </div>
            <button
              type="button"
              disabled={!feedbackReady}
              onClick={afterFeedback}
              aria-label={pickedOption.correct ? "Далі" : "Спробувати ще"}
              className={`flex size-26 cursor-pointer items-center justify-center rounded-full border-7 border-white text-white shadow-[0_12px_0_var(--color-kid-teal-shadow)] disabled:opacity-40 ${
                solved
                  ? "bg-kid-teal animate-[breathe_2.2s_ease-in-out_infinite]"
                  : "bg-kid-sun text-kid-sun-deep shadow-[0_12px_0_var(--color-kid-sun-shadow)]"
              }`}
            >
              {pickedOption.correct ? (
                <svg width="52" height="52" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <path d="M6 12h11M12.5 6.5 18 12l-5.5 5.5" stroke="currentColor" strokeWidth="2.8" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              ) : (
                <svg width="52" height="52" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <path d="M19 12a7 7 0 1 1-2.05-4.95M19 4.5V8h-3.5" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              )}
            </button>
          </div>
        ) : null}
      </div>
    </KidStage>
  );
}
