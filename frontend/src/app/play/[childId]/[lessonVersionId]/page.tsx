"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { apiFetch, ApiError } from "@/lib/api";
import { PlaythroughEngine } from "@/components/scene-engine/PlaythroughEngine";
import type { LessonScenario } from "@/lib/scenario-types";

interface StartAttemptResponse {
  attemptId: string;
}

type LoadState =
  | { status: "loading" }
  | { status: "error"; error: ApiError }
  | { status: "ready"; attemptId: string; scenario: LessonScenario }
  | { status: "done"; score: number };

export default function PlaythroughPage() {
  const { childId, lessonVersionId } = useParams<{ childId: string; lessonVersionId: string }>();
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
        const { attemptId } = await apiFetch<StartAttemptResponse>(`/children/${childId}/attempts`, {
          method: "POST",
          body: JSON.stringify({ lessonVersionId }),
        });
        const scenario = await apiFetch<LessonScenario>(`/children/${childId}/lessons/${lessonVersionId}/scenario`);
        setState({ status: "ready", attemptId, scenario });
      } catch (err) {
        setState({ status: "error", error: err instanceof ApiError ? err : new ApiError(0, "Unexpected error") });
      }
    })();
  }, [childId, lessonVersionId]);

  if (state.status === "loading") {
    return (
      <main className="flex flex-1 flex-col items-center justify-center gap-4 p-8">
        <span className="text-6xl">🦊</span>
        <p className="text-xl">Тоша готує урок…</p>
      </main>
    );
  }

  if (state.status === "error") {
    return (
      <main className="flex flex-1 flex-col items-center justify-center gap-4 p-8 text-center">
        <p className="text-xl text-red-600">{state.error.message}</p>
        <Link href={`/play/${childId}`} className="text-blue-600 underline">
          ← Обрати інший урок
        </Link>
      </main>
    );
  }

  if (state.status === "done") {
    return (
      <main className="flex flex-1 flex-col items-center justify-center gap-6 p-8 text-center">
        <span className="text-8xl">🎉</span>
        <h1 className="text-3xl font-bold">Молодець!</h1>
        <Link
          href="/play"
          className="min-h-16 rounded-full bg-blue-600 px-12 py-5 text-2xl font-bold text-white shadow-lg active:scale-95"
        >
          Далі
        </Link>
      </main>
    );
  }

  return (
    <main className="flex flex-1 flex-col">
      <PlaythroughEngine
        scenario={state.scenario}
        attemptId={state.attemptId}
        onComplete={(score) => setState({ status: "done", score })}
      />
    </main>
  );
}
