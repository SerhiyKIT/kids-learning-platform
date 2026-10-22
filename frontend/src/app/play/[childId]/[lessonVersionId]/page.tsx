"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import { ApiError } from "@/lib/api";
import type { PlayState } from "@/lib/api-types";
import { ExitButton, KidStage, Mascot } from "@/components/kid/KidChrome";
import { LessonFinale } from "@/components/kid/LessonFinale";
import { SleepScreen } from "@/components/kid/SleepScreen";
import { useKidMode } from "@/components/kid/kid-mode-context";
import { useKidAudio } from "@/components/scene-engine/KidAudio";
import { PlaythroughEngine } from "@/components/scene-engine/PlaythroughEngine";
import { voiceLineAudioUrls, type LessonScenario } from "@/lib/scenario-types";



type LoadState =
  | { status: "loading" }
  | { status: "resting" }
  | { status: "error" }
  | { status: "ready"; attemptId: string; scenario: LessonScenario; lessonId: string; earnedBefore: boolean }
  | { status: "done"; scenario: LessonScenario; lessonId: string; earnedBefore: boolean; resting: boolean };

/**
 * One lesson inside child mode (the layout has already unlocked sound with «Почнімо!», so the
 * first scene starts right away). Starting the attempt is where the server enforces the rest: a
 * 409 CHILD_RESTING shows the sleeping screen, never an error.
 */
export default function PlaythroughPage() {
  const { childId, lessonVersionId } = useParams<{ childId: string; lessonVersionId: string }>();
  const router = useRouter();
  const { preload } = useKidAudio();
  const { startLesson } = useKidMode();
  const [state, setState] = useState<LoadState>({ status: "loading" });
  // Starting an attempt is a real POST with a side effect, so it must run at most once — even
  // under React StrictMode's dev-only mount/cleanup/remount cycle, which reuses this same ref
  // across both passes. (A `cancelled`-on-cleanup flag doesn't fit here: StrictMode's synthetic
  // cleanup would flip it before this one-and-only fetch resolves, discarding its result with no
  // second attempt left to replace it.)
  const started = useRef(false);

  useEffect(() => {
    if (started.current) return;
    started.current = true;
    (async () => {
      try {
        // Usually already under way since the tap on the shelf (QA1-036).
        const { attemptId, lessonId, scenario, earnedBefore } = await startLesson(lessonVersionId);
        if (scenario.scenes[0]) preload(voiceLineAudioUrls(scenario.scenes[0]));
        setState({ status: "ready", attemptId, scenario, lessonId, earnedBefore });
      } catch (err) {
        setState(err instanceof ApiError && err.code === "CHILD_RESTING" ? { status: "resting" } : { status: "error" });
      }
    })();
  }, [lessonVersionId, preload, startLesson]);

  const backToShelf = useCallback(() => router.push(`/play/${childId}`), [router, childId]);

  const onComplete = useCallback((playState: PlayState | null) => {
    setState((current) =>
      current.status === "ready"
        ? {
            status: "done",
            scenario: current.scenario,
            lessonId: current.lessonId,
            earnedBefore: current.earnedBefore,
            resting: playState?.resting ?? false,
          }
        : current,
    );
  }, []);

  if (state.status === "resting") {
    return <SleepScreen onWake={backToShelf} />;
  }

  if (state.status === "loading" || state.status === "error") {
    return (
      <KidStage>
        <ExitButton />
        <div className="relative z-2 flex h-full w-full flex-col items-center justify-center gap-6">
          <Mascot className="h-[clamp(120px,46dvh,240px)] w-[clamp(100px,38dvh,200px)]" />
          {state.status === "error" ? (
            <Link
              href={`/play/${childId}`}
              aria-label="До уроків"
              className="bg-kid-sun text-kid-sun-deep flex size-[clamp(68px,16dvh,92px)] items-center justify-center rounded-[28%] border-[clamp(4px,1dvh,6px)] border-white no-underline shadow-[0_10px_0_var(--color-kid-sun-shadow)]"
            >
              <svg viewBox="0 0 24 24" fill="none" className="size-[56%]" aria-hidden="true">
                <path d="M4 11.2 12 4.5l8 6.7V19a1.5 1.5 0 0 1-1.5 1.5H15v-5.5H9v5.5H5.5A1.5 1.5 0 0 1 4 19v-7.8Z" fill="currentColor" />
              </svg>
            </Link>
          ) : null}
        </div>
      </KidStage>
    );
  }

  if (state.status === "done") {
    return (
      <LessonFinale
        lessonId={state.lessonId}
        topic={state.scenario.topic}
        bridge={state.scenario.reality_bridge}
        firstTime={!state.earnedBefore}
        resting={state.resting}
        onBackToShelf={backToShelf}
      />
    );
  }

  return <PlaythroughEngine scenario={state.scenario} attemptId={state.attemptId} onComplete={onComplete} />;
}
