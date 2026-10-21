"use client";

import { isChoiceScene, isDemoScene, isDialogScene, isSortingScene, type Scene } from "@/lib/scenario-types";
import { ChoiceSceneView } from "./ChoiceSceneView";
import { DemoSceneView } from "./DemoSceneView";
import { DialogSceneView } from "./DialogSceneView";
import { FallbackSceneView } from "./FallbackSceneView";
import { SortingSceneView } from "./SortingSceneView";

export interface AnswerEvent {
  sceneKey: string;
  tryNo: number;
  chosenOption: string;
  isCorrect: boolean;
  hintsUsed: number;
}

/** What every scene view receives from the playthrough engine. */
export interface SceneProps<S extends Scene = Scene> {
  scene: S;
  progress?: { index: number; total: number };
  /** Every attempt, recorded as it happens (POST …/answers). */
  onAnswer: (event: AnswerEvent) => void;
  /** Once, when an interactive scene is finished: was it right on the first try? Demo scenes never call it. */
  onScored: (firstTryCorrect: boolean) => void;
  onAdvance: () => void;
}

export function SceneRenderer(props: SceneProps) {
  const { scene } = props;
  if (isDemoScene(scene)) return <DemoSceneView {...props} scene={scene} />;
  if (isChoiceScene(scene)) return <ChoiceSceneView {...props} scene={scene} />;
  if (isSortingScene(scene)) return <SortingSceneView {...props} scene={scene} />;
  if (isDialogScene(scene)) return <DialogSceneView {...props} scene={scene} />;
  // Only for scene types the schema doesn't define yet.
  return <FallbackSceneView scene={scene} onAdvance={props.onAdvance} />;
}
