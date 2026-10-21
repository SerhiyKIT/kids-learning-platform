"use client";

import { useEffect, useRef, useState } from "react";
import { apiFetch } from "@/lib/api";
import type { PlayState } from "@/lib/api-types";
import { useKidAudio } from "@/components/scene-engine/KidAudio";
import { ExitButton, KidStage, Mascot } from "./KidChrome";
import { useKidMode } from "./kid-mode-context";

/** How often the sleeping screen checks whether the rest is over (or a parent woke the child). */
const WAKE_CHECK_MS = 60_000;

/**
 * «Тоша спить»: the child is resting after the session limit (docs/UX_гайд_дитячого_режиму.md §6).
 * Dimmed, slowly breathing, one soft line — and no clock or countdown anywhere. It quietly checks
 * the server now and then and calls onWake once the rest is over or a parent has woken the child.
 *
 * @param voiced whether to say the "sleeping" line (false right after the lesson-end farewell,
 *   which already said goodbye)
 */
export function SleepScreen({ voiced = true, onWake }: { voiced?: boolean; onWake: () => void }) {
  const { childId, line } = useKidMode();
  const { play } = useKidAudio();
  const [dim, setDim] = useState(false);
  const said = useRef(false);
  const sleepLine = line("sleep_again")?.audioUrl;

  useEffect(() => {
    const frame = requestAnimationFrame(() => setDim(true));
    return () => cancelAnimationFrame(frame);
  }, []);

  useEffect(() => {
    if (voiced && sleepLine && !said.current) {
      said.current = true;
      void play(sleepLine);
    }
  }, [voiced, sleepLine, play]);

  useEffect(() => {
    const check = setInterval(() => {
      apiFetch<PlayState>(`/children/${childId}/play-state`)
        .then((state) => {
          if (!state.resting) onWake();
        })
        .catch(() => {});
    }, WAKE_CHECK_MS);
    return () => clearInterval(check);
  }, [childId, onWake]);

  return (
    <KidStage>
      <div
        aria-hidden="true"
        className={`absolute inset-0 z-1 bg-[radial-gradient(110%_90%_at_50%_40%,#3a4170_0%,#232848_70%,#191d36_100%)] transition-opacity duration-[1600ms] ease-out ${
          dim ? "opacity-92" : "opacity-0"
        }`}
      />
      <ExitButton />
      <div
        role="img"
        aria-label="Тоша спить"
        className="relative z-2 flex h-full w-full items-center justify-center"
        data-sleeping="1"
      >
        <div className="relative">
          <Mascot className="h-[clamp(130px,50dvh,280px)] w-[clamp(110px,42dvh,236px)] animate-[sleepbreathe_6s_ease-in-out_infinite]! opacity-80 saturate-50 motion-reduce:animate-none!" />
          {[0, 1, 2].map((i) => (
            <span
              key={i}
              aria-hidden="true"
              className="absolute top-[4%] right-[-8%] font-semibold text-white/80 opacity-0 animate-[zfloat_4.5s_ease-out_infinite] motion-reduce:hidden"
              style={{ animationDelay: `${i * 1.5}s`, fontSize: `clamp(18px, ${4 + i}dvh, ${30 + i * 8}px)` }}
            >
              z
            </span>
          ))}
        </div>
      </div>
    </KidStage>
  );
}
