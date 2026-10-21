"use client";

import { useEffect, useRef, useState } from "react";
import type { Assistant, VoiceLine } from "@/lib/scenario-types";
import { stripeFor } from "@/components/kid/KidChrome";
import { useKidAudio } from "./KidAudio";
import type { AnswerEvent } from "./SceneRenderer";
import { NextButton, SceneShell } from "./SceneShell";
import { useSceneAssistant } from "./useSceneAssistant";
import { VoiceLinePlayer } from "./VoiceLinePlayer";

export interface PickOption {
  id: string;
  icon: string;
  label: VoiceLine;
  correct: boolean;
  feedback: VoiceLine;
}

type Phase = "choosing" | "feedback";

/**
 * "Pick the one right card" — the shared engine of choice and dialog scenes. Wrong answers are
 * never a dead end (docs/UX_гайд_дитячого_режиму.md §1/§8): the feedback plays, the next voiced hint
 * follows (not on control scenes — the engine turns hints off there, per the schema), and the child
 * tries again; after hint 2 the correct card is gently highlighted. The attention pause escalates
 * the same way (useSceneAssistant).
 *
 * @param readOptionsAloud dialog: after the question, each answer is read out in turn with its card
 * highlighted, so a child who can't read still knows what every picture says.
 */
export function PickOneScene({
  sceneKey,
  background,
  isControl,
  prompt,
  options,
  assistant,
  readOptionsAloud = false,
  progress,
  onAnswer,
  onScored,
  onAdvance,
}: {
  sceneKey: string;
  background: string;
  isControl: boolean;
  prompt: VoiceLine;
  options: PickOption[];
  assistant: Assistant;
  readOptionsAloud?: boolean;
  progress?: { index: number; total: number };
  onAnswer: (event: AnswerEvent) => void;
  onScored: (firstTryCorrect: boolean) => void;
  onAdvance: () => void;
}) {
  const { playToEnd } = useKidAudio();
  const [tryNo, setTryNo] = useState(1);
  const [phase, setPhase] = useState<Phase>("choosing");
  const [picked, setPicked] = useState<PickOption | null>(null);
  const [feedbackReady, setFeedbackReady] = useState(false);
  const [readingId, setReadingId] = useState<string | null>(null);
  const [readingAloud, setReadingAloud] = useState(readOptionsAloud);
  // A wrong card "bounces" instead of locking the screen.
  const [nudged, setNudged] = useState<{ id: string; n: number } | null>(null);
  const nudgeTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const phaseRef = useRef<Phase>("choosing");

  const assistantState = useSceneAssistant({ assistant, isControl, waiting: phase === "choosing" && !readingAloud });
  const { hint, revealCorrect, touch, registerMistake, guideTap, hintLevel } = assistantState;

  useEffect(
    () => () => {
      if (nudgeTimer.current) clearTimeout(nudgeTimer.current);
    },
    [],
  );

  // Dialog: question, then every answer in turn. Any tap (a choice, the guide) ends the sequence.
  useEffect(() => {
    if (!readOptionsAloud) return;
    let cancelled = false;
    (async () => {
      const sequence = [prompt, ...options.map((o) => o.label)];
      for (const [i, line] of sequence.entries()) {
        if (cancelled || phaseRef.current !== "choosing") break;
        if (!line.audioUrl) continue;
        setReadingId(i === 0 ? null : options[i - 1].id);
        if (!(await playToEnd(line.audioUrl))) break;
      }
      if (!cancelled) {
        setReadingId(null);
        setReadingAloud(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [readOptionsAloud, prompt, options, playToEnd]);

  function choose(option: PickOption) {
    if (phase !== "choosing") return;
    touch();
    if (!option.correct) {
      setNudged((prev) => ({ id: option.id, n: (prev?.n ?? 0) + 1 }));
      if (nudgeTimer.current) clearTimeout(nudgeTimer.current);
      nudgeTimer.current = setTimeout(() => setNudged(null), 700);
    }
    phaseRef.current = "feedback";
    setReadingId(null);
    setReadingAloud(false);
    setPicked(option);
    setFeedbackReady(false);
    setPhase("feedback");
    onAnswer({ sceneKey, tryNo, chosenOption: option.id, isCorrect: option.correct, hintsUsed: hintLevel });
  }

  function afterFeedback() {
    if (!picked) return;
    if (picked.correct) {
      onScored(tryNo === 1);
      onAdvance();
      return;
    }
    setTryNo((n) => n + 1);
    registerMistake();
    phaseRef.current = "choosing";
    setPhase("choosing");
    setPicked(null);
  }

  return (
    <SceneShell
      background={background}
      caption={<VoiceLinePlayer key={prompt.key} line={prompt} variant="caption" autoPlay={!readOptionsAloud} />}
      hint={phase === "choosing" ? hint : undefined}
      onGuideTap={guideTap}
      progress={progress}
    >
      {phase === "choosing" ? (
        <div className="flex w-full items-end justify-center gap-[clamp(12px,2.5vw,24px)]">
          {options.map((option) => {
            const isNudged = nudged?.id === option.id;
            const revealed = revealCorrect && option.correct;
            const reading = readingId === option.id;
            return (
              <div key={option.id} className="relative max-w-75 min-w-[max(15vw,64px)] flex-1">
                {revealed ? (
                  <span
                    aria-hidden="true"
                    className="bg-kid-correct absolute -inset-[clamp(6px,1.5dvh,16px)] animate-[glowring_1.6s_ease-in-out_infinite] rounded-[48px] motion-reduce:animate-none"
                  />
                ) : null}
                <button
                  type="button"
                  onClick={() => choose(option)}
                  aria-label={option.label.text}
                  className={`relative flex w-full cursor-pointer flex-col items-center gap-[clamp(4px,1.2dvh,10px)] rounded-[clamp(22px,6dvh,40px)] border-[clamp(4px,1dvh,7px)] p-[clamp(6px,1.6dvh,14px)] shadow-[0_10px_0_var(--color-kid-card-shadow)] transition-transform duration-200 motion-reduce:animate-none ${
                    revealed ? "border-kid-correct bg-kid-correct-surface" : "border-white bg-white"
                  } ${reading ? "scale-105 ring-4 ring-[color:var(--color-kid-sun)]" : ""} ${
                    isNudged ? `animate-[softbounce_620ms_ease-out_${nudged.n}]` : "animate-[breathe_2.6s_ease-in-out_infinite]"
                  }`}
                >
                  <span
                    className="flex h-[clamp(36px,13dvh,106px)] w-full items-center justify-center rounded-[clamp(14px,4dvh,24px)]"
                    style={{ background: stripeFor(option.icon) }}
                    role="img"
                    aria-label={`Малюнок: ${option.icon}`}
                  />
                  <span className="text-kid-ink text-center text-[clamp(15px,4.4dvh,23px)] leading-tight font-semibold">
                    {option.label.text}
                  </span>
                </button>
              </div>
            );
          })}
        </div>
      ) : picked ? (
        <div className="flex w-full items-center justify-center gap-[clamp(12px,3vw,32px)]">
          <div className="max-w-[60vw]">
            <VoiceLinePlayer
              key={picked.feedback.key}
              line={picked.feedback}
              variant="caption"
              onReady={() => setFeedbackReady(true)}
            />
          </div>
          <NextButton kind={picked.correct ? "next" : "retry"} disabled={!feedbackReady} onClick={afterFeedback} />
        </div>
      ) : null}
    </SceneShell>
  );
}
