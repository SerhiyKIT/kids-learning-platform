"use client";

import { isChoiceScene, isDemoScene, type Scene } from "@/lib/scenario-types";
import { ChoiceSceneView } from "./ChoiceSceneView";
import { DemoSceneView } from "./DemoSceneView";
import { FallbackSceneView } from "./FallbackSceneView";

export interface AnswerEvent {
  sceneKey: string;
  tryNo: number;
  chosenOption: string;
  isCorrect: boolean;
  hintsUsed: number;
}

export function SceneRenderer({
  scene,
  onAdvance,
  onAnswer,
}: {
  scene: Scene;
  onAdvance: () => void;
  onAnswer: (event: AnswerEvent) => void;
}) {
  if (isDemoScene(scene)) {
    return <DemoSceneView scene={scene} onAdvance={onAdvance} />;
  }
  if (isChoiceScene(scene)) {
    return <ChoiceSceneView scene={scene} onAdvance={onAdvance} onAnswer={onAnswer} />;
  }
  return <FallbackSceneView scene={scene} onAdvance={onAdvance} />;
}
