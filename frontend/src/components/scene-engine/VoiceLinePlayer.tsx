"use client";

import { useEffect, useRef, useState } from "react";
import type { VoiceLine } from "@/lib/scenario-types";
import { useKidAudio, useSpeakingUrl } from "./KidAudio";

const NO_AUDIO_PAUSE_MS = 1200;

/**
 * Voice-first per docs/UX_гайд_дитячого_режиму.md §4/§1: a line plays its audio the moment it
 * appears, with a big replay button. If the browser refuses to start it (autoplay policy, muted
 * device) there's no error — the replay button just keeps pulsing until tapped. No audio at all
 * (line not voiced) just shows the text and reports "not ready" for a brief pause instead of
 * letting the child instantly skip past something they had no time to see or hear.
 */
export function VoiceLinePlayer({
  line,
  onReady,
}: {
  line: VoiceLine;
  /** Fires once, after the line has had a moment to be seen/heard. */
  onReady?: () => void;
}) {
  const { play } = useKidAudio();
  const speakingUrl = useSpeakingUrl();
  // `line` is stable for this component's lifetime — every call site keys VoiceLinePlayer on
  // line.key, so a new line means a fresh mount, not a prop change on this instance.
  const [ready, setReady] = useState(() => Boolean(line.audioUrl));
  const [needsTap, setNeedsTap] = useState(false);
  // onReady is an inline callback at every call site; keep the latest without re-firing on it.
  const onReadyRef = useRef(onReady);
  useEffect(() => {
    onReadyRef.current = onReady;
  });

  useEffect(() => {
    if (line.audioUrl) return;
    const timer = setTimeout(() => setReady(true), NO_AUDIO_PAUSE_MS);
    return () => clearTimeout(timer);
  }, [line.audioUrl]);

  useEffect(() => {
    if (ready) onReadyRef.current?.();
  }, [ready]);

  useEffect(() => {
    if (!line.audioUrl) return;
    let active = true;
    play(line.audioUrl).then((started) => {
      if (active && !started) setNeedsTap(true);
    });
    return () => {
      active = false;
    };
  }, [play, line.audioUrl]);

  function onListen() {
    if (!line.audioUrl) return;
    play(line.audioUrl, { restart: true }).then((started) => setNeedsTap(!started));
  }

  const speaking = Boolean(line.audioUrl) && speakingUrl === line.audioUrl;

  return (
    <div className="flex w-full items-center gap-4 rounded-2xl bg-white/95 px-6 py-4 shadow-lg">
      <p className="flex-1 text-2xl leading-snug font-semibold text-slate-800">{line.text}</p>
      {line.audioUrl ? (
        <button
          type="button"
          onClick={onListen}
          aria-label="Прослухати ще раз"
          className={`flex h-16 w-16 shrink-0 cursor-pointer items-center justify-center rounded-full bg-blue-500 text-3xl text-white active:scale-95 ${
            speaking
              ? "animate-[talk_0.45s_ease-in-out_infinite]"
              : needsTap
                ? "animate-[pulsedot_1.6s_ease-in-out_infinite] ring-4 ring-blue-200"
                : ""
          }`}
        >
          🔊
        </button>
      ) : null}
    </div>
  );
}
