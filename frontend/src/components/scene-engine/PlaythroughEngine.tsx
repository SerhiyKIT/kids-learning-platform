"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { apiFetch } from "@/lib/api";
import { voiceLineAudioUrls, type LessonScenario } from "@/lib/scenario-types";
import { useKidAudio } from "./KidAudio";
import { type AnswerEvent, SceneRenderer } from "./SceneRenderer";

/**
 * Drives one lesson playthrough: current scene, per-scene answer recording, and — on the last
 * scene — completing the attempt with a score. Score is the fraction of interactive scenes (choice,
 * sorting, dialog) done right on the FIRST try, as each scene reports it when it finishes: for
 * choice/dialog the first answer was right; for sorting every item went into the right basket on
 * its first attempt. The backend trusts this value, by design.
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
  const firstTryResults = useRef<Map<string, boolean>>(new Map());

  const scene = scenario.scenes[sceneIndex];
  const isLastScene = sceneIndex === scenario.scenes.length - 1;
  const { preload } = useKidAudio();

  // While the child is in this scene, fetch the next scene's lines so it starts without silence.
  useEffect(() => {
    const next = scenario.scenes[sceneIndex + 1];
    if (next) preload(voiceLineAudioUrls(next));
  }, [scenario, sceneIndex, preload]);

  const recordAnswer = useCallback(
    (event: AnswerEvent) => {
      void apiFetch(`/attempts/${attemptId}/answers`, { method: "POST", body: JSON.stringify(event) }).catch(() => {
        // Best-effort: a lost answer write shouldn't block the child mid-lesson.
      });
    },
    [attemptId],
  );

  const recordScore = useCallback(
    (firstTryCorrect: boolean) => {
      if (scene && !firstTryResults.current.has(scene.key)) {
        firstTryResults.current.set(scene.key, firstTryCorrect);
      }
    },
    [scene],
  );

  const advance = useCallback(() => {
    if (!isLastScene) {
      setSceneIndex((i) => i + 1);
      return;
    }
    const results = [...firstTryResults.current.values()];
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

  return (
    <SceneRenderer
      key={scene.key}
      scene={scene}
      progress={{ index: sceneIndex, total: scenario.scenes.length }}
      onAdvance={advance}
      onAnswer={recordAnswer}
      onScored={recordScore}
    />
  );
}
