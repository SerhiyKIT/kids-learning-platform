"use client";

import { useEffect, useRef, useState } from "react";
import type { VoiceLine } from "@/lib/scenario-types";

const NO_AUDIO_PAUSE_MS = 1200;

/**
 * Voice-first per docs/UX_гайд_дитячого_режиму.md §4/§1: a line always plays its audio the
 * moment it appears, with a big replay button. No audio yet (see VoiceLine.audioUrl) just shows
 * the text and reports "not ready" for a brief pause instead of letting the child instantly skip
 * past something they had no time to see or hear.
 */
export function VoiceLinePlayer({
  line,
  onReady,
}: {
  line: VoiceLine;
  /** Fires once, after the line has had a moment to be seen/heard. */
  onReady?: () => void;
}) {
  const audioRef = useRef<HTMLAudioElement | null>(null);
  // `line` is stable for this component's lifetime — every call site keys VoiceLinePlayer on
  // line.key, so a new line means a fresh mount, not a prop change on this instance.
  const [ready, setReady] = useState(() => Boolean(line.audioUrl));

  useEffect(() => {
    if (line.audioUrl) {
      return;
    }
    const timer = setTimeout(() => setReady(true), NO_AUDIO_PAUSE_MS);
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (ready) onReady?.();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ready]);

  useEffect(() => {
    if (line.audioUrl && audioRef.current) {
      audioRef.current.play().catch(() => {});
    }
  }, [line.audioUrl, line.key]);

  return (
    <div className="flex w-full items-center gap-4 rounded-2xl bg-white/95 px-6 py-4 shadow-lg">
      <p className="flex-1 text-2xl leading-snug font-semibold text-slate-800">{line.text}</p>
      {line.audioUrl ? (
        <>
          <audio ref={audioRef} src={line.audioUrl} />
          <button
            type="button"
            onClick={() => audioRef.current?.play()}
            aria-label="Прослухати ще раз"
            className="flex h-16 w-16 shrink-0 items-center justify-center rounded-full bg-blue-500 text-3xl text-white active:scale-95"
          >
            🔊
          </button>
        </>
      ) : null}
    </div>
  );
}
