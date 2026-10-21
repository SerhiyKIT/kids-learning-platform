"use client";

import { useState } from "react";
import type { DemoScene } from "@/lib/scenario-types";
import { useKidAudio } from "./KidAudio";
import type { SceneProps } from "./SceneRenderer";
import { NextButton, SceneShell } from "./SceneShell";
import { VoiceLinePlayer } from "./VoiceLinePlayer";

/** Narration scene: the guide tells the story line by line; «Далі» after each line. Nothing scored. */
export function DemoSceneView({ scene, progress, onAdvance }: SceneProps<DemoScene>) {
  const { replay } = useKidAudio();
  const [lineIndex, setLineIndex] = useState(0);
  const [ready, setReady] = useState(false);
  const currentLine = scene.narration[lineIndex];
  const isLastLine = lineIndex === scene.narration.length - 1;

  return (
    <SceneShell
      background={scene.background}
      caption={
        currentLine ? (
          <VoiceLinePlayer key={currentLine.line.key} line={currentLine.line} variant="caption" onReady={() => setReady(true)} />
        ) : null
      }
      onGuideTap={replay}
      progress={progress}
    >
      <NextButton
        kind="next"
        disabled={!ready}
        onClick={() => {
          setReady(false);
          if (isLastLine) {
            onAdvance();
          } else {
            setLineIndex((i) => i + 1);
          }
        }}
      />
    </SceneShell>
  );
}
