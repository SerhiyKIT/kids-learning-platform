import { apiFetch } from "@/lib/api";
import type { StickersResponse } from "@/lib/api-types";
import type { LessonScenario } from "@/lib/scenario-types";

interface StartAttemptResponse {
  attemptId: string;
  lessonId: string;
}

export interface LessonStart {
  attemptId: string;
  lessonId: string;
  scenario: LessonScenario;
  /** The lesson's sticker was already in the album: this is a replay. */
  earnedBefore: boolean;
}

/**
 * Everything a lesson needs to begin, in one round trip (QA1-036): the attempt (a 409
 * CHILD_RESTING rejects it), the scenario — which doesn't depend on the attempt — and the album,
 * which only decides "new sticker" or "replay" and never fails the lesson. The attempt's answer
 * names the lesson, so the list of lessons isn't needed.
 */
export async function fetchLessonStart(childId: string, lessonVersionId: string): Promise<LessonStart> {
  const [{ attemptId, lessonId }, scenario, album] = await Promise.all([
    apiFetch<StartAttemptResponse>(`/children/${childId}/attempts`, {
      method: "POST",
      body: JSON.stringify({ lessonVersionId }),
    }),
    apiFetch<LessonScenario>(`/children/${childId}/lessons/${lessonVersionId}/scenario`),
    apiFetch<StickersResponse>(`/children/${childId}/stickers`).catch(() => null),
  ]);
  return { attemptId, lessonId, scenario, earnedBefore: album?.stickers.some((s) => s.lessonId === lessonId) ?? false };
}
