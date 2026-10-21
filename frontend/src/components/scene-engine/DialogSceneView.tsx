"use client";

import { useMemo } from "react";
import type { DialogScene } from "@/lib/scenario-types";
import { PickOneScene, type PickOption } from "./PickOneScene";
import type { SceneProps } from "./SceneRenderer";

/**
 * Dialog question: the guide asks, the child answers by tapping a picture (voice answers aren't in
 * the MVP). Answers are read out one after another right after the question, each card lifting as
 * it's named — a preschooler can't read the labels, and reading them on tap would turn every
 * answer into a two-tap gesture. Otherwise the same pick-one pedagogy as choice scenes.
 */
export function DialogSceneView({ scene, ...props }: SceneProps<DialogScene>) {
  const options = useMemo<PickOption[]>(
    () => scene.answers.map((a) => ({ id: a.id, icon: a.icon, label: a.label, correct: a.correct, feedback: a.feedback })),
    [scene],
  );
  return (
    <PickOneScene
      sceneKey={scene.key}
      background={scene.background}
      isControl={scene.is_control}
      prompt={scene.question}
      options={options}
      assistant={scene.assistant}
      readOptionsAloud
      {...props}
    />
  );
}
