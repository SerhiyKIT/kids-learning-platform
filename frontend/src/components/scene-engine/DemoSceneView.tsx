"use client";

import { useState } from "react";
import type { DemoScene } from "@/lib/scenario-types";
import { SceneAsset } from "./SceneAsset";
import { VoiceLinePlayer } from "./VoiceLinePlayer";

/** Narration scene: background + character placeholders, voice lines in order, then "Далі". */
export function DemoSceneView({ scene, onAdvance }: { scene: DemoScene; onAdvance: () => void }) {
  const [lineIndex, setLineIndex] = useState(0);
  const [ready, setReady] = useState(false);
  const currentLine = scene.narration[lineIndex];
  const isLastLine = lineIndex === scene.narration.length - 1;

  return (
    <div className="flex h-full w-full flex-col gap-4 p-4">
      <div className="relative min-h-0 flex-1">
        <SceneAsset kind="background" id={scene.background} className="absolute inset-0" />
        <div className="absolute bottom-4 left-1/2 flex -translate-x-1/2 gap-4">
          {scene.characters.map((character) => (
            <SceneAsset
              key={character.id}
              kind="character"
              id={`${character.id} (${character.emotion})`}
              className="h-32 w-32"
            />
          ))}
        </div>
      </div>

      {currentLine ? (
        <VoiceLinePlayer key={currentLine.line.key} line={currentLine.line} onReady={() => setReady(true)} />
      ) : null}

      <button
        type="button"
        disabled={!ready}
        onClick={() => {
          setReady(false);
          if (isLastLine) {
            onAdvance();
          } else {
            setLineIndex((i) => i + 1);
          }
        }}
        className="min-h-16 self-center rounded-full bg-blue-600 px-12 py-5 text-2xl font-bold text-white shadow-lg active:scale-95 disabled:opacity-40"
      >
        Далі
      </button>
    </div>
  );
}
