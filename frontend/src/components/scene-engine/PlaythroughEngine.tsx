"use client";

import { useCallback, useRef, useState } from "react";
import { apiFetch } from "@/lib/api";
import type { LessonScenario } from "@/lib/scenario-types";
import { type AnswerEvent, SceneRenderer } from "./SceneRenderer";

/**
 * Drives one lesson playthrough: current scene, per-scene answer recording, and — on the last
 * scene — completing the attempt with a score. Score is the fraction of choice/control scenes
 * answered correctly on the FIRST try (the backend trusts this value, by design).
 */
export function PlaythroughEngine({
  scenario,
  attemptId,
  onComplete,
}: {
  scenario: LessonScenario;
  attemptId: string;
  onComplete: (score: number) => void;
}) {
  const [sceneIndex, setSceneIndex] = useState(0);
  const firstTryResults = useRef<boolean[]>([]);
  const scoredSceneKeys = useRef<Set<string>>(new Set());

  const scene = scenario.scenes[sceneIndex];
  const isLastScene = sceneIndex === scenario.scenes.length - 1;

  const recordAnswer = useCallback(
    (event: AnswerEvent) => {
      if (event.tryNo === 1 && !scoredSceneKeys.current.has(event.sceneKey)) {
        scoredSceneKeys.current.add(event.sceneKey);
        firstTryResults.current.push(event.isCorrect);
      }
      void apiFetch(`/attempts/${attemptId}/answers`, { method: "POST", body: JSON.stringify(event) }).catch(() => {
        // Best-effort: a lost answer write shouldn't block the child mid-lesson.
      });
    },
    [attemptId],
  );

  const advance = useCallback(() => {
    if (!isLastScene) {
      setSceneIndex((i) => i + 1);
      return;
    }
    const results = firstTryResults.current;
    const score = results.length > 0 ? results.filter(Boolean).length / results.length : 1;
    void apiFetch(`/attempts/${attemptId}/complete`, {
      method: "POST",
      body: JSON.stringify({ result: "completed", score }),
    })
      .catch(() => {
        // Still celebrate locally even if this particular write failed.
      })
      .finally(() => onComplete(score));
  }, [isLastScene, attemptId, onComplete]);

  if (!scene) {
    return null;
  }

  return <SceneRenderer key={scene.key} scene={scene} onAdvance={advance} onAnswer={recordAnswer} />;
}
