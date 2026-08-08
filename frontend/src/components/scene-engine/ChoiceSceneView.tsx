"use client";

import { useState } from "react";
import type { ChoiceOption, ChoiceScene } from "@/lib/scenario-types";
import { SceneAsset } from "./SceneAsset";
import type { AnswerEvent } from "./SceneRenderer";
import { VoiceLinePlayer } from "./VoiceLinePlayer";

type Phase = "choosing" | "feedback";

/**
 * Choice/control scene: setup line + large tappable option cards. Wrong answers are never a
 * dead end (docs/UX_гайд_дитячого_режиму.md §1/§8): feedback plays, the next assistant hint
 * appears (skipped on control scenes — the engine turns hints off there, per the schema), and
 * the child tries again. After both hint levels are used up, the correct card is gently
 * highlighted instead of a third silent hint.
 */
export function ChoiceSceneView({
  scene,
  onAdvance,
  onAnswer,
}: {
  scene: ChoiceScene;
  onAdvance: () => void;
  onAnswer: (event: AnswerEvent) => void;
}) {
  const [tryNo, setTryNo] = useState(1);
  const [hintLevel, setHintLevel] = useState(0);
  const [revealCorrect, setRevealCorrect] = useState(false);
  const [phase, setPhase] = useState<Phase>("choosing");
  const [pickedOption, setPickedOption] = useState<ChoiceOption | null>(null);
  const [feedbackReady, setFeedbackReady] = useState(false);

  const hint = !scene.is_control && hintLevel > 0 ? scene.assistant.hints.find((h) => h.level === hintLevel) : undefined;

  function choose(option: ChoiceOption) {
    if (phase !== "choosing") return;
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

  return (
    <div className="flex h-full w-full flex-col gap-4 p-4">
      <div className="relative h-32 shrink-0">
        <SceneAsset kind="background" id={scene.background} className="absolute inset-0" />
      </div>

      <VoiceLinePlayer key={scene.setup.key} line={scene.setup} />

      {hint && phase === "choosing" ? (
        <div className="rounded-2xl bg-amber-100 px-4 py-3 text-xl font-medium text-amber-900">💡 {hint.line.text}</div>
      ) : null}

      {phase === "choosing" ? (
        <div className="grid flex-1 auto-rows-fr grid-cols-2 gap-4">
          {scene.options.map((option) => (
            <button
              key={option.id}
              type="button"
              onClick={() => choose(option)}
              className={`flex min-h-32 flex-col items-center justify-center gap-2 rounded-3xl border-4 p-4 shadow-md active:scale-95 ${
                revealCorrect && option.correct
                  ? "animate-pulse border-emerald-500 bg-emerald-50"
                  : "border-black/10 bg-white"
              }`}
            >
              <SceneAsset kind="icon" id={option.icon} className="h-24 w-24" />
              <span className="text-xl font-semibold text-slate-800">{option.label.text}</span>
            </button>
          ))}
        </div>
      ) : pickedOption ? (
        <div className="flex flex-1 flex-col items-center justify-center gap-6">
          <VoiceLinePlayer
            key={pickedOption.feedback.line.key}
            line={pickedOption.feedback.line}
            onReady={() => setFeedbackReady(true)}
          />
          <span className="text-6xl">{pickedOption.correct ? "🎉" : "🙂"}</span>
          <button
            type="button"
            disabled={!feedbackReady}
            onClick={afterFeedback}
            className="min-h-16 rounded-full bg-blue-600 px-12 py-5 text-2xl font-bold text-white shadow-lg active:scale-95 disabled:opacity-40"
          >
            {pickedOption.correct ? "Далі" : "Спробувати ще"}
          </button>
        </div>
      ) : null}
    </div>
  );
}
