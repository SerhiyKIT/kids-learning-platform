"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import type { Assistant, VoiceLine } from "@/lib/scenario-types";
import { useKidAudio } from "./KidAudio";

/** docs/UX_гайд_дитячого_режиму.md §8 «пауза уваги»: ~10 s idle → hint 1, ~15 s more → hint 2, … */
export const IDLE_FIRST_HINT_MS = 10_000;
export const IDLE_NEXT_HINT_MS = 15_000;

export interface SceneAssistant {
  /** 0 = no hint yet, 1–2 = the hint level reached. */
  hintLevel: number;
  /** The current hint's line (its text is the adult's dub of what the guide just said). */
  hint: VoiceLine | undefined;
  /** After hint 2 the engine itself gently shows the correct action. Never on control scenes. */
  revealCorrect: boolean;
  /** Any touch by the child: restarts the attention pause. */
  touch: () => void;
  /** A wrong answer: escalates the hint level, exactly like an attention pause does. */
  registerMistake: () => void;
  /** Tap on the guide: re_explain (if voiced), otherwise the current line again. */
  guideTap: () => void;
}

/**
 * The scripted helper behind every interactive scene (choice, sorting, dialog): voiced hints that
 * escalate after a mistake OR an attention pause, then an automatic "show me" of the correct
 * action. Hints are heard through the shared lesson audio (KidAudio); text is only a dub for the
 * adult. Control scenes (is_control) test what was learned, so there are no hints, no reveal and
 * no re_explain there — the guide only repeats the current line.
 *
 * @param waiting whether the scene is currently waiting for the child (e.g. not while feedback is
 * playing); the attention-pause timer only runs while waiting.
 */
export function useSceneAssistant({
  assistant,
  isControl,
  waiting,
}: {
  assistant: Assistant;
  isControl: boolean;
  waiting: boolean;
}): SceneAssistant {
  const { play, replay } = useKidAudio();
  const [hintLevel, setHintLevel] = useState(0);
  const [revealCorrect, setRevealCorrect] = useState(false);
  const [touches, setTouches] = useState(0);
  // The level is also read synchronously by escalate(), outside React's render cycle.
  const levelRef = useRef(0);
  const hintsEnabled = !isControl;

  const escalate = useCallback(() => {
    if (!hintsEnabled) return;
    const level = levelRef.current;
    if (level >= 2) {
      setRevealCorrect(true);
      return;
    }
    const next = level + 1;
    levelRef.current = next;
    setHintLevel(next);
    const line = assistant.hints.find((h) => h.level === next)?.line;
    if (line?.audioUrl) void play(line.audioUrl, { restart: true });
  }, [hintsEnabled, assistant, play]);

  const touch = useCallback(() => setTouches((n) => n + 1), []);

  const registerMistake = useCallback(() => {
    touch();
    escalate();
  }, [touch, escalate]);

  const guideTap = useCallback(() => {
    touch();
    const reExplain = assistant.re_explain;
    if (hintsEnabled && reExplain.audioUrl) {
      void play(reExplain.audioUrl, { restart: true });
    } else {
      replay();
    }
  }, [touch, hintsEnabled, assistant, play, replay]);

  // Attention pause: restarts on every touch and after every escalation; cleared on unmount.
  useEffect(() => {
    if (!hintsEnabled || !waiting || revealCorrect) return;
    const timer = setTimeout(escalate, hintLevel === 0 ? IDLE_FIRST_HINT_MS : IDLE_NEXT_HINT_MS);
    return () => clearTimeout(timer);
  }, [hintsEnabled, waiting, revealCorrect, hintLevel, touches, escalate]);

  const hint = hintsEnabled && hintLevel > 0 ? assistant.hints.find((h) => h.level === hintLevel)?.line : undefined;

  return { hintLevel, hint, revealCorrect, touch, registerMistake, guideTap };
}
