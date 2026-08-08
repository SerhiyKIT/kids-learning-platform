"use client";

import { useState } from "react";
import type { Scene, VoiceLine } from "@/lib/scenario-types";
import { VoiceLinePlayer } from "./VoiceLinePlayer";

/**
 * Any scene type the renderer doesn't have a dedicated view for yet (e.g. sorting/dialog):
 * recursively finds every {key,text} voice line in the scene — same heuristic the backend's
 * ScenarioValidator uses — and presents them in order with a "Далі" button. No interaction
 * recorded, since we don't know this shape's answer semantics.
 */
export function FallbackSceneView({ scene, onAdvance }: { scene: Scene; onAdvance: () => void }) {
  const [lines] = useState(() => collectVoiceLines(scene));
  const [index, setIndex] = useState(0);
  const [ready, setReady] = useState(lines.length === 0);
  const current = lines[index];
  const isLast = index === lines.length - 1;

  return (
    <div className="flex h-full w-full flex-col items-center justify-center gap-6 p-4">
      {current ? (
        <VoiceLinePlayer key={current.key} line={current} onReady={() => setReady(true)} />
      ) : (
        <p className="text-xl text-slate-600">…</p>
      )}
      <button
        type="button"
        disabled={!ready}
        onClick={() => {
          setReady(false);
          if (isLast) {
            onAdvance();
          } else {
            setIndex((i) => i + 1);
          }
        }}
        className="min-h-16 rounded-full bg-blue-600 px-12 py-5 text-2xl font-bold text-white shadow-lg active:scale-95 disabled:opacity-40"
      >
        Далі
      </button>
    </div>
  );
}

function collectVoiceLines(node: unknown): VoiceLine[] {
  const lines: VoiceLine[] = [];
  walk(node, lines);
  return lines;
}

function walk(node: unknown, lines: VoiceLine[]): void {
  if (Array.isArray(node)) {
    for (const item of node) walk(item, lines);
    return;
  }
  if (node && typeof node === "object") {
    const record = node as Record<string, unknown>;
    if (typeof record.key === "string" && typeof record.text === "string") {
      lines.push(record as unknown as VoiceLine);
      return;
    }
    for (const value of Object.values(record)) walk(value, lines);
  }
}
