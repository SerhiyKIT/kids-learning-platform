"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import { apiFetch } from "@/lib/api";
import {
  COLOR_IDS,
  COLOR_LABELS,
  COLOR_SPOTS,
  MOOD_IDS,
  MOOD_LABELS,
  WORLD_IDS,
  WORLD_LABELS,
  type ColorId,
  type MoodId,
  type StyleResponse,
  type WorldId,
} from "@/lib/theme-catalog";
import { ExitButton, KidStage, ListenButton, Mascot, StepDots } from "@/components/kid/KidChrome";
import { WorldDecor } from "@/components/kid/KidTheme";
import { useKidMode, type ShellLineKey } from "@/components/kid/kid-mode-context";
import { useKidAudio, useSpeakingUrl } from "@/components/scene-engine/KidAudio";

type Step = "color" | "world" | "mood" | "drawing" | "ready";

const QUESTION: Record<"color" | "world" | "mood", ShellLineKey> = {
  color: "style_color",
  world: "style_world",
  mood: "style_mood",
};

/** Minimum time a picked card stays highlighted before the next question, even without audio. */
const PICK_MS = 900;
const FAREWELL_MS = 2600;

/**
 * The child picks a style (docs/UX_гайд_дитячого_режиму.md: zero reading — pictures and the
 * guide's voice): a colour (6 spots) → a world (6 cards) → a mood (2 cards). The answers are just
 * catalog ids. If that combination's theme is already approved it's applied right away; otherwise
 * the guide says it's "drawing the room" and the child keeps the current theme until a moderator
 * approves the new one. Only reachable with the parent's style consent (the shelf shows the brush
 * then); without it the server refuses and the child is simply taken back.
 */
export default function StylePickerPage() {
  const { childId } = useParams<{ childId: string }>();
  const router = useRouter();
  const { line, setStyle } = useKidMode();
  const { play, playToEnd } = useKidAudio();
  const speaking = useSpeakingUrl() !== null;
  const [step, setStep] = useState<Step>("color");
  const [color, setColor] = useState<ColorId | null>(null);
  const [world, setWorld] = useState<WorldId | null>(null);
  const [picked, setPicked] = useState<string | null>(null);
  // The latest tap wins: an earlier pick still being said doesn't advance the step.
  const pickToken = useRef(0);

  const backToShelf = useCallback(() => router.push(`/play/${childId}`), [router, childId]);

  const questionUrl = step === "color" || step === "world" || step === "mood" ? line(QUESTION[step])?.audioUrl : undefined;
  useEffect(() => {
    if (questionUrl) void play(questionUrl);
  }, [questionUrl, play]);

  const say = useCallback(
    async (key: ShellLineKey, minMs: number) => {
      const startedAt = performance.now();
      const url = line(key)?.audioUrl;
      if (url) await playToEnd(url);
      const left = minMs - (performance.now() - startedAt);
      if (left > 0) await new Promise((resolve) => setTimeout(resolve, left));
    },
    [line, playToEnd],
  );

  async function finish(mood: MoodId) {
    if (!color || !world) return;
    let saved: StyleResponse;
    try {
      saved = await apiFetch<StyleResponse>(`/children/${childId}/style`, {
        method: "PUT",
        body: JSON.stringify({ colorId: color, worldId: world, moodId: mood }),
      });
    } catch {
      // Consent withdrawn meanwhile, or offline: nothing to show the child — back to the lessons.
      backToShelf();
      return;
    }
    if (saved.pending) {
      setStyle(saved);
      setStep("drawing");
      await say("style_drawing", FAREWELL_MS);
    } else {
      // Already approved for these answers: the room changes right now and the guide says so.
      setStyle({ ...saved, announce: false });
      setStep("ready");
      void apiFetch(`/children/${childId}/style/seen`, { method: "POST" }).catch(() => {});
      await say("theme_ready", FAREWELL_MS);
    }
    backToShelf();
  }

  async function pick(kind: "color" | "world" | "mood", id: string) {
    const token = ++pickToken.current;
    setPicked(id);
    await say(`${kind}_${id}` as ShellLineKey, PICK_MS);
    if (token !== pickToken.current) return;
    setPicked(null);
    if (kind === "color") {
      setColor(id as ColorId);
      setStep("world");
    } else if (kind === "world") {
      setWorld(id as WorldId);
      setStep("mood");
    } else {
      void finish(id as MoodId);
    }
  }

  const onGuideTap = () => (questionUrl ? void play(questionUrl, { restart: true }) : undefined);
  const stepIndex = step === "color" ? 0 : step === "world" ? 1 : 2;
  const card = (id: string) =>
    `relative flex cursor-pointer items-center justify-center overflow-hidden rounded-[clamp(18px,5dvh,34px)] border-[clamp(4px,1.2dvh,7px)] bg-white shadow-[0_10px_0_var(--color-kid-card-shadow)] transition-transform duration-150 ${
      picked === id ? "border-kid-teal scale-105" : "border-white"
    }`;

  return (
    <KidStage>
      <ExitButton />
      <div className="relative z-2 flex h-full w-full flex-col px-[clamp(20px,3vw,28px)] pt-[clamp(8px,2dvh,20px)] pb-[clamp(22px,4dvh,30px)]">
        <StepDots total={3} current={stepIndex} />
        <div className="flex min-h-0 flex-1 items-stretch gap-[clamp(10px,2.5vw,26px)] pt-[clamp(4px,1.5dvh,10px)] pr-[clamp(48px,6vw,64px)]">
          <div className="flex flex-none flex-col items-center justify-center gap-[clamp(6px,2dvh,14px)]">
            <button type="button" onClick={onGuideTap} aria-label="Провідник: запитати ще раз" className="cursor-pointer">
              <Mascot speaking={speaking} className="h-[clamp(64px,24dvh,168px)] w-[clamp(54px,20dvh,140px)]" />
            </button>
            {step === "drawing" || step === "ready" ? null : (
              <>
                <ListenButton label="Послухати ще раз" onClick={onGuideTap} speaking={speaking} className="size-[clamp(68px,14dvh,96px)]" />
                <Link
                  href={`/play/${childId}`}
                  aria-label="До уроків"
                  className="bg-kid-sun text-kid-sun-deep flex size-[clamp(68px,14dvh,92px)] items-center justify-center rounded-[28%] border-[clamp(4px,1dvh,6px)] border-white no-underline shadow-[0_10px_0_var(--color-kid-sun-shadow)] [@media(max-height:500px)]:hidden"
                >
                  <svg viewBox="0 0 24 24" fill="none" className="size-[56%]" aria-hidden="true">
                    <path d="M4 11.2 12 4.5l8 6.7V19a1.5 1.5 0 0 1-1.5 1.5H15v-5.5H9v5.5H5.5A1.5 1.5 0 0 1 4 19v-7.8Z" fill="currentColor" />
                  </svg>
                </Link>
              </>
            )}
          </div>

          <div className="flex min-w-0 flex-1 items-center justify-center" data-step={step}>
            {step === "color" ? (
              <div className="grid h-full max-h-[min(100%,560px)] w-full grid-cols-3 grid-rows-2 gap-[clamp(10px,2.5dvh,22px)]">
                {COLOR_IDS.map((id) => (
                  <button key={id} type="button" aria-label={`Колір: ${COLOR_LABELS[id]}`} data-pick={id} onClick={() => void pick("color", id)} className={card(id)}>
                    <ColorSpot color={COLOR_SPOTS[id]} />
                  </button>
                ))}
              </div>
            ) : null}
            {step === "world" ? (
              <div className="grid h-full max-h-[min(100%,560px)] w-full grid-cols-3 grid-rows-2 gap-[clamp(10px,2.5dvh,22px)]">
                {WORLD_IDS.map((id) => (
                  <button key={id} type="button" aria-label={`Світ: ${WORLD_LABELS[id]}`} data-pick={id} onClick={() => void pick("world", id)} className={card(id)}>
                    <WorldDecor worldId={id} />
                    <WorldIcon world={id} />
                  </button>
                ))}
              </div>
            ) : null}
            {step === "mood" ? (
              <div className="grid h-full max-h-[min(100%,420px)] w-full grid-cols-2 gap-[clamp(14px,4vw,40px)]">
                {MOOD_IDS.map((id) => (
                  <button key={id} type="button" aria-label={`Настрій: ${MOOD_LABELS[id]}`} data-pick={id} onClick={() => void pick("mood", id)} className={card(id)}>
                    <MoodPicture mood={id} />
                  </button>
                ))}
              </div>
            ) : null}
            {step === "drawing" ? (
              <div role="img" aria-label="Провідник малює кімнату" className="flex items-center justify-center" data-drawing="1">
                <DrawingPicture />
              </div>
            ) : null}
          </div>
        </div>
      </div>
    </KidStage>
  );
}

function ColorSpot({ color }: { color: string }) {
  return (
    <svg viewBox="0 0 100 80" className="size-[78%]" aria-hidden="true">
      <path d="M50 6c22 0 42 10 42 30 0 24-18 38-44 38C22 74 8 60 8 40 8 20 28 6 50 6Z" fill={color} />
      <ellipse cx="34" cy="26" rx="10" ry="6" fill="#ffffff" opacity="0.45" />
    </svg>
  );
}

/** Big pictograms of the worlds — placeholders like the decor (drawn from theme tokens). */
function WorldIcon({ world }: { world: WorldId }) {
  const body = { fill: "var(--color-kid-teal)" };
  const accent = { fill: "var(--color-kid-sun)" };
  const ink = { fill: "var(--color-kid-ink)" };
  return (
    <svg viewBox="0 0 64 64" className="relative size-[62%]" aria-hidden="true">
      {world === "space" ? (
        <g>
          <path d="M32 6c9 8 12 20 9 34H23C20 26 23 14 32 6Z" style={body} />
          <circle cx="32" cy="24" r="5" style={{ fill: "var(--color-kid-bg-top)" }} />
          <path d="M23 40l-7 10 9-3zM41 40l7 10-9-3z" style={accent} />
          <path d="M28 44h8l-4 12z" style={accent} />
        </g>
      ) : null}
      {world === "sea" ? (
        <g>
          <path d="M8 32c10-14 30-14 40 0-10 14-30 14-40 0Z" style={body} />
          <path d="M46 32l12-10v20z" style={body} />
          <circle cx="20" cy="29" r="3" style={ink} />
        </g>
      ) : null}
      {world === "forest" ? (
        <g>
          <path d="M32 6l18 30H14z" style={body} />
          <path d="M32 20l20 30H12z" style={body} />
          <rect x="28" y="50" width="8" height="10" style={ink} />
        </g>
      ) : null}
      {world === "dinosaurs" ? (
        <g>
          <path d="M10 46c0-14 10-22 24-22h6c6 0 10-6 16-6v10c-4 0-6 4-6 10v8z" style={body} />
          <rect x="16" y="44" width="7" height="12" rx="3" style={body} />
          <rect x="34" y="44" width="7" height="12" rx="3" style={body} />
          <path d="M20 24l4-8 4 8 4-8 4 8z" style={accent} />
          <circle cx="52" cy="22" r="2" style={ink} />
        </g>
      ) : null}
      {world === "flowers" ? (
        <g>
          {[0, 72, 144, 216, 288].map((a) => (
            <circle key={a} cx={32 + 13 * Math.cos((a * Math.PI) / 180)} cy={26 + 13 * Math.sin((a * Math.PI) / 180)} r="9" style={accent} />
          ))}
          <circle cx="32" cy="26" r="7" style={body} />
          <rect x="30" y="38" width="4" height="20" style={body} />
        </g>
      ) : null}
      {world === "cars" ? (
        <g>
          <rect x="6" y="30" width="52" height="16" rx="7" style={body} />
          <path d="M16 30l8-12h18l8 12z" style={body} />
          <rect x="26" y="21" width="12" height="8" rx="2" style={{ fill: "var(--color-kid-bg-top)" }} />
          <circle cx="18" cy="48" r="7" style={ink} />
          <circle cx="46" cy="48" r="7" style={ink} />
        </g>
      ) : null}
    </svg>
  );
}

/** Calm: a soft moon and clouds in pale colours; bright: a sun with rays and confetti. */
function MoodPicture({ mood }: { mood: MoodId }) {
  if (mood === "calm") {
    return (
      <svg viewBox="0 0 100 80" className="size-[80%]" aria-hidden="true">
        <rect width="100" height="80" rx="10" fill="#e7eef7" />
        <path d="M60 14a18 18 0 1 0 14 30 15 15 0 1 1-14-30Z" fill="#c9d4ea" />
        <ellipse cx="30" cy="56" rx="18" ry="8" fill="#ffffff" />
        <ellipse cx="44" cy="52" rx="12" ry="7" fill="#ffffff" />
      </svg>
    );
  }
  return (
    <svg viewBox="0 0 100 80" className="size-[80%]" aria-hidden="true">
      <rect width="100" height="80" rx="10" fill="#fff4cf" />
      {[0, 45, 90, 135, 180, 225, 270, 315].map((a) => (
        <rect key={a} x="48" y="14" width="4" height="10" rx="2" fill="#f59a23" transform={`rotate(${a} 50 40)`} />
      ))}
      <circle cx="50" cy="40" r="12" fill="#ffc13b" />
      <rect x="14" y="14" width="6" height="6" rx="1" fill="#e8574c" transform="rotate(20 17 17)" />
      <rect x="80" y="58" width="6" height="6" rx="1" fill="#3d8fe0" transform="rotate(-15 83 61)" />
      <rect x="78" y="12" width="6" height="6" rx="1" fill="#4fb86a" />
      <rect x="18" y="60" width="6" height="6" rx="1" fill="#9a63d6" transform="rotate(35 21 63)" />
    </svg>
  );
}

/** The guide "drawing the room": a brush over an easel — a picture, no words. */
function DrawingPicture() {
  return (
    <svg viewBox="0 0 120 100" className="size-[min(60vw,52dvh)] animate-[talk_1.2s_ease-in-out_infinite] motion-reduce:animate-none" aria-hidden="true">
      <rect x="20" y="10" width="80" height="60" rx="6" fill="#ffffff" style={{ stroke: "var(--color-kid-ink-soft)" }} strokeWidth="4" />
      <path d="M30 56c10-20 22-24 30-12s20 8 30-14" fill="none" style={{ stroke: "var(--color-kid-teal)" }} strokeWidth="6" strokeLinecap="round" />
      <path d="M40 70 30 94M80 70l10 24" style={{ stroke: "var(--color-kid-ink-soft)" }} strokeWidth="4" strokeLinecap="round" />
      <path d="M86 18 106 2l6 6-16 20z" style={{ fill: "var(--color-kid-sun)" }} />
    </svg>
  );
}
