"use client";

import { useEffect } from "react";
import { voiceLineAudioUrls, type LessonScenario } from "@/lib/scenario-types";
import { ExitButton, KidStage, ListenButton, Mascot } from "@/components/kid/KidChrome";
import { useKidAudio } from "./KidAudio";

/**
 * «Почнімо!» — the one tap before the first scene. Browsers only allow sound after a user gesture,
 * so this tap unlocks audio for the whole lesson (starting the first line inside the gesture when
 * it's already downloaded) and then starts the lesson. Picture + sound only, no reading needed.
 */
export function LessonStartScreen({ scenario, onStart }: { scenario: LessonScenario; onStart: () => void }) {
  const { preload, unlock } = useKidAudio();
  const firstUrl = scenario.scenes[0] ? voiceLineAudioUrls(scenario.scenes[0])[0] : undefined;

  // Download the first scene while the child looks at the start screen.
  useEffect(() => {
    if (scenario.scenes[0]) preload(voiceLineAudioUrls(scenario.scenes[0]));
  }, [scenario, preload]);

  function start() {
    unlock(firstUrl);
    onStart();
  }

  return (
    <KidStage>
      <ExitButton />
      <div className="relative z-2 flex h-full w-full flex-col items-center justify-center gap-8 landscape:flex-row landscape:gap-16">
        <button type="button" onClick={start} aria-label="Почнімо!" className="cursor-pointer">
          <Mascot size="lg" />
        </button>
        <ListenButton size="lg" label="Почнімо!" onClick={start} animation="breathe" />
      </div>
    </KidStage>
  );
}
