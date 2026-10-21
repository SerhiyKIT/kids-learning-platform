"use client";

import { useMemo } from "react";
import type { ChoiceScene } from "@/lib/scenario-types";
import { PickOneScene, type PickOption } from "./PickOneScene";
import type { SceneProps } from "./SceneRenderer";

/**
 * Choice/control scene: setup line + large tappable option cards. Pedagogy (feedback → hint →
 * retry → gentle reveal of the correct card after hint 2, no hints on control scenes) lives in
 * PickOneScene/useSceneAssistant, shared with dialog scenes.
 */
export function ChoiceSceneView({ scene, ...props }: SceneProps<ChoiceScene>) {
  const options = useMemo<PickOption[]>(
    () =>
      scene.options.map((o) => ({ id: o.id, icon: o.icon, label: o.label, correct: o.correct, feedback: o.feedback.line })),
    [scene],
  );
  return (
    <PickOneScene
      sceneKey={scene.key}
      background={scene.background}
      isControl={scene.is_control}
      prompt={scene.setup}
      options={options}
      assistant={scene.assistant}
      {...props}
    />
  );
}
