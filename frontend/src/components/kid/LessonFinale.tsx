"use client";

import { useEffect, useLayoutEffect, useRef, useState } from "react";
import type { VoiceLine } from "@/lib/scenario-types";
import { useKidAudio, useSpeakingUrl } from "@/components/scene-engine/KidAudio";
import { ExitButton, KidStage, Mascot } from "./KidChrome";
import { SleepScreen } from "./SleepScreen";
import { AlbumIcon, StickerArt } from "./Stickers";
import { useKidMode } from "./kid-mode-context";

/** Minimum time each step stays on screen, also when its line has no audio (yet). */
const CELEBRATE_MS = 3500;
const STICKER_MS = 1600;
const FLY_MS = 1100;
const BRIDGE_MS = 2000;
const FAREWELL_MS = 3200;

type Phase = "celebrate" | "sticker" | "fly" | "bridge" | "farewell" | "sleep";

const CONFETTI_COLORS = ["#ffc13b", "#2fb3a3", "#ff8a65", "#7aa7ff", "#f58ad0", "#8ede9f"];

/**
 * The end of a lesson (docs/UX_гайд_дитячого_режиму.md §5–§7): a short celebration (3–5 s), the
 * sticker handed over and flying into the album (first completion only — a replay is celebrated by
 * the guide, the sticker isn't duplicated), the lesson's reality bridge, and back to the shelf. If
 * this lesson used up the session, the guide says goodbye and the screen folds into the sleeping one.
 */
export function LessonFinale({
  lessonId,
  topic,
  bridge,
  firstTime,
  resting,
  onBackToShelf,
}: {
  lessonId: string;
  topic: string;
  bridge: VoiceLine;
  firstTime: boolean;
  resting: boolean;
  onBackToShelf: () => void;
}) {
  const { line } = useKidMode();
  const { playToEnd } = useKidAudio();
  const speaking = useSpeakingUrl() !== null;
  const [phase, setPhase] = useState<Phase>("celebrate");
  const stickerRef = useRef<HTMLSpanElement>(null);
  const albumRef = useRef<HTMLSpanElement>(null);
  const [flyTo, setFlyTo] = useState<{ x: number; y: number } | null>(null);

  // The sequence runs once; it reads the latest values through this ref.
  const latest = useRef({ line, playToEnd, bridge, firstTime, resting, onBackToShelf });
  useLayoutEffect(() => {
    latest.current = { line, playToEnd, bridge, firstTime, resting, onBackToShelf };
  });

  useEffect(() => {
    let cancelled = false;
    const timers = new Set<ReturnType<typeof setTimeout>>();
    const wait = (ms: number) =>
      new Promise<void>((resolve) => {
        const timer = setTimeout(() => {
          timers.delete(timer);
          resolve();
        }, ms);
        timers.add(timer);
      });
    const say = async (url: string | undefined, minMs: number) => {
      const startedAt = performance.now();
      if (url) await latest.current.playToEnd(url);
      const left = minMs - (performance.now() - startedAt);
      if (left > 0) await wait(left);
    };
    const step = (next: Phase) => {
      if (!cancelled) setPhase(next);
      return !cancelled;
    };

    (async () => {
      const { line: lineOf, bridge: bridgeLine, firstTime: first, resting: rest } = latest.current;
      await say(lineOf(first ? "celebrate" : "replay_joy")?.audioUrl, CELEBRATE_MS);
      if (first) {
        if (!step("sticker")) return;
        await say(lineOf("sticker_new")?.audioUrl, STICKER_MS);
        if (!step("fly")) return;
        await wait(FLY_MS);
      }
      if (!step("bridge")) return;
      await say(bridgeLine.audioUrl, BRIDGE_MS);
      if (cancelled) return;
      if (!rest) {
        latest.current.onBackToShelf();
        return;
      }
      if (!step("farewell")) return;
      await say(lineOf("sleep_tired")?.audioUrl, FAREWELL_MS);
      step("sleep");
    })();

    return () => {
      cancelled = true;
      timers.forEach(clearTimeout);
    };
  }, []);

  // Where the sticker flies: from its own centre to the album icon's.
  useLayoutEffect(() => {
    if (phase !== "fly" || !stickerRef.current || !albumRef.current) return;
    const from = stickerRef.current.getBoundingClientRect();
    const to = albumRef.current.getBoundingClientRect();
    setFlyTo({
      x: to.left + to.width / 2 - (from.left + from.width / 2),
      y: to.top + to.height / 2 - (from.top + from.height / 2),
    });
  }, [phase]);

  if (phase === "sleep") {
    return <SleepScreen voiced={false} onWake={onBackToShelf} />;
  }

  const showSticker = phase === "sticker" || phase === "fly";
  const stickerLanded = firstTime && (phase === "bridge" || phase === "farewell");

  return (
    <KidStage>
      <ExitButton />
      {phase === "celebrate" ? <Confetti /> : null}

      <div
        aria-hidden="true"
        className={`pointer-events-none absolute inset-0 z-3 bg-[#232848] transition-opacity duration-[3000ms] ease-in ${
          phase === "farewell" ? "opacity-80" : "opacity-0"
        }`}
      />

      <div className="relative z-2 flex h-full w-full items-center justify-center gap-[clamp(20px,8vw,96px)] px-[clamp(20px,4vw,40px)]" data-phase={phase}>
        <div
          className={
            phase === "celebrate"
              ? "animate-[yay_0.9s_ease-in-out_infinite_alternate] motion-reduce:animate-none"
              : phase === "farewell"
                ? "origin-bottom animate-[wave_1.6s_ease-in-out_infinite] motion-reduce:animate-none"
                : ""
          }
        >
          <Mascot speaking={speaking} className="h-[clamp(120px,48dvh,280px)] w-[clamp(100px,40dvh,236px)]" />
        </div>

        {showSticker ? (
          <span
            ref={stickerRef}
            role="img"
            aria-label="Нова наліпка"
            data-sticker-new="1"
            className={`block ${
              phase === "fly" && flyTo
                ? "animate-[stickerfly_1s_ease-in_forwards]"
                : "animate-[stickerpop_0.7s_ease-out_forwards]"
            }`}
            style={flyTo ? ({ "--fly-x": `${flyTo.x}px`, "--fly-y": `${flyTo.y}px` } as React.CSSProperties) : undefined}
          >
            <StickerArt seed={lessonId} topic={topic} className="size-[clamp(110px,38dvh,220px)]" />
          </span>
        ) : null}
      </div>

      {/* The album the sticker flies into — a picture here, not a control. */}
      {firstTime ? (
        <span
          ref={albumRef}
          aria-hidden="true"
          className={`bg-kid-teal absolute right-[clamp(20px,3vw,28px)] bottom-[clamp(22px,4dvh,30px)] z-2 flex size-[clamp(64px,16dvh,88px)] items-center justify-center rounded-[28%] border-[clamp(4px,1dvh,6px)] border-white text-white shadow-[0_10px_0_var(--color-kid-teal-shadow)] ${
            stickerLanded ? "animate-[pulsedot_0.6s_ease-in-out_2]" : ""
          }`}
        >
          <AlbumIcon className="size-[58%]" />
        </span>
      ) : null}
    </KidStage>
  );
}

function Confetti() {
  return (
    <div aria-hidden="true" className="pointer-events-none absolute inset-0 z-1 overflow-hidden motion-reduce:hidden">
      {Array.from({ length: 22 }, (_, i) => (
        <span
          key={i}
          className="absolute top-0 block animate-[confetti_2.8s_linear_infinite] rounded-[3px]"
          style={{
            left: `${(i * 47) % 100}%`,
            width: `${8 + (i % 3) * 4}px`,
            height: `${14 + (i % 4) * 3}px`,
            background: CONFETTI_COLORS[i % CONFETTI_COLORS.length],
            animationDelay: `${((i * 0.37) % 2.4).toFixed(2)}s`,
          }}
        />
      ))}
    </div>
  );
}
