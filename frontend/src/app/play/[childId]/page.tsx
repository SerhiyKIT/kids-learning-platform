"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import { apiFetch, ApiError } from "@/lib/api";
import type { PlayState, StickersResponse } from "@/lib/api-types";
import { DoneStamp, ExitButton, KidStage, ListenButton, Mascot, stripeFor } from "@/components/kid/KidChrome";
import { SleepScreen } from "@/components/kid/SleepScreen";
import { AlbumLink, StyleLink } from "@/components/kid/Stickers";
import { useKidMode, type ShellLineKey } from "@/components/kid/kid-mode-context";
import { useKidAudio, useSpeakingUrl } from "@/components/scene-engine/KidAudio";

interface AvailableLesson {
  lessonVersionId: string;
  lessonId: string;
  title: string;
  moduleCode: string;
  assignmentId: string;
  dueAt: string | null;
}

const GREETINGS: ShellLineKey[] = ["greet_1", "greet_2", "greet_3"];
// Kept for this page load only (no browser storage): coming back from a lesson never repeats the
// greeting just heard.
let lastGreeting: ShellLineKey | null = null;

function pickGreeting(): ShellLineKey {
  const options = GREETINGS.filter((g) => g !== lastGreeting);
  return options[Math.floor(Math.random() * options.length)];
}

/**
 * The lesson shelf (docs/UX_гайд_дитячого_режиму.md §5): big picture cards with no titles to read
 * (the title is only the aria-label), passed ones stamped, the next one "breathing" and centred; the
 * guide greets the child out loud. While the child is resting, the sleeping screen instead.
 */
export default function ShelfPage() {
  const { childId } = useParams<{ childId: string }>();
  const { line, style, styleReady, setStyle, prepareLesson } = useKidMode();
  const { play, replay } = useKidAudio();
  const speaking = useSpeakingUrl() !== null;
  const [lessons, setLessons] = useState<AvailableLesson[] | null>(null);
  const [earned, setEarned] = useState<Set<string>>(new Set());
  const [resting, setResting] = useState<boolean | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  const [greeting] = useState(pickGreeting);
  const greeted = useRef(false);
  const shelfRef = useRef<HTMLDivElement>(null);

  const load = useCallback(() => {
    apiFetch<PlayState>(`/children/${childId}/play-state`)
      .then((state) => setResting(state.resting))
      // Not knowing the play state must not lock the child out: the lesson start re-checks anyway.
      .catch(() => setResting(false));
    apiFetch<AvailableLesson[]>(`/children/${childId}/available-lessons`)
      .then(setLessons)
      .catch((err) => setError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка")));
    // Stickers only stamp the cards — their failure mustn't break the shelf.
    apiFetch<StickersResponse>(`/children/${childId}/stickers`)
      .then((album) => setEarned(new Set(album.stickers.map((s) => s.lessonId))))
      .catch(() => setEarned(new Set()));
  }, [childId]);

  useEffect(() => {
    load();
  }, [load]);

  const greetingUrl = line(greeting)?.audioUrl;
  const announceUrl = line("theme_ready")?.audioUrl;
  useEffect(() => {
    if (resting !== false || !styleReady || greeted.current) return;
    // A newly approved theme: the guide happily announces it (once) instead of the greeting.
    if (style?.announce) {
      greeted.current = true;
      if (announceUrl) void play(announceUrl);
      setStyle({ ...style, announce: false });
      void apiFetch(`/children/${childId}/style/seen`, { method: "POST" }).catch(() => {});
      return;
    }
    if (greetingUrl) {
      greeted.current = true;
      lastGreeting = greeting;
      void play(greetingUrl);
    }
  }, [resting, styleReady, style, setStyle, announceUrl, childId, greeting, greetingUrl, play]);

  // The child doesn't hunt for where to continue — the next lesson centres itself.
  const centerNext = useCallback(() => {
    const shelf = shelfRef.current;
    const next = shelf?.querySelector<HTMLElement>('[data-next="1"]');
    if (!shelf || !next) return;
    shelf.scrollLeft = Math.max(0, next.offsetLeft - (shelf.clientWidth - next.offsetWidth) / 2);
  }, []);

  useEffect(() => {
    if (!lessons) return;
    const frame = requestAnimationFrame(centerNext);
    window.addEventListener("resize", centerNext);
    return () => {
      cancelAnimationFrame(frame);
      window.removeEventListener("resize", centerNext);
    };
  }, [lessons, centerNext]);

  if (resting) {
    return <SleepScreen onWake={() => setResting(false)} />;
  }

  const nextIndex = lessons?.findIndex((l) => !earned.has(l.lessonId)) ?? -1;
  const onGuideTap = () => (greetingUrl ? void play(greetingUrl, { restart: true }) : replay());

  return (
    <KidStage>
      <ExitButton />

      <aside className="relative z-2 flex h-full flex-none flex-col items-center justify-center gap-[clamp(8px,3dvh,24px)] py-[clamp(16px,5dvh,40px)] pr-2 pl-[clamp(20px,3vw,32px)]">
        <button type="button" onClick={onGuideTap} aria-label="Провідник: привітатися ще раз" className="cursor-pointer">
          <Mascot speaking={speaking} className="h-[clamp(84px,30dvh,240px)] w-[clamp(70px,25dvh,200px)]" />
        </button>
        <div className="flex items-center gap-[clamp(10px,2.5dvh,20px)] [@media(min-height:560px)]:flex-col">
          <ListenButton label="Послухати ще раз" onClick={onGuideTap} speaking={speaking} className="size-[clamp(68px,16dvh,96px)]" />
          <AlbumLink href={`/play/${childId}/stickers`} />
          {style?.consented ? <StyleLink href={`/play/${childId}/style`} /> : null}
        </div>
      </aside>

      <section className="relative z-2 flex h-full min-w-0 flex-1 flex-col justify-center">
        {error ? (
          // The only text on the shelf, and it's for the adult: the child can't fix this anyway.
          <p className="text-kid-ink px-14 text-2xl font-semibold">Уроки не завантажились. Покажи це дорослому.</p>
        ) : null}
        {!lessons && !error ? (
          <div className="flex gap-6 px-6" aria-label="Готуємо уроки">
            {[0, 1, 2].map((i) => (
              <span key={i} className="h-[clamp(140px,56dvh,344px)] w-[clamp(108px,43dvh,264px)] flex-none animate-pulse rounded-[40px] bg-white/50" />
            ))}
          </div>
        ) : null}
        {lessons && lessons.length === 0 ? (
          <div role="img" aria-label="Поки що немає уроків" className="flex justify-center">
            <span className="h-[clamp(140px,56dvh,344px)] w-[clamp(108px,43dvh,264px)] rounded-[40px] border-7 border-dashed border-white/80" />
          </div>
        ) : null}

        {lessons && lessons.length > 0 ? (
          <>
            <div
              ref={shelfRef}
              className="kid-shelf flex snap-x snap-mandatory items-center gap-[clamp(14px,3vw,26px)] overflow-x-auto overflow-y-hidden pt-[clamp(22px,6dvh,40px)] pr-[clamp(56px,7vw,72px)] pb-[clamp(18px,5dvh,34px)] pl-4 [mask-image:linear-gradient(to_right,transparent_0,#000_40px,#000_calc(100%-90px),transparent_100%)]"
            >
              {lessons.map((lesson, i) => {
                const done = earned.has(lesson.lessonId);
                const isNext = i === nextIndex;
                return (
                  <div
                    key={lesson.lessonVersionId}
                    data-next={isNext ? "1" : "0"}
                    data-done={done ? "1" : "0"}
                    className="relative flex-none snap-center pt-2.5"
                  >
                    {isNext ? (
                      <span
                        aria-hidden="true"
                        className="bg-kid-glow absolute inset-x-1.5 -top-3.5 bottom-1.5 animate-[glowring_2.4s_ease-in-out_infinite] rounded-[44px] motion-reduce:animate-none"
                      />
                    ) : null}
                    <Link
                      href={`/play/${childId}/${lesson.lessonVersionId}`}
                      // The lesson screen is fetched while the shelf is on display, so a tap starts
                      // the lesson's own requests straight away (QA1-036).
                      prefetch
                      onClick={() => prepareLesson(lesson.lessonVersionId)}
                      aria-label={done ? `${lesson.title} — пройдено` : `Грати урок ${lesson.title}`}
                      className={`relative block h-[clamp(140px,56dvh,344px)] w-[clamp(108px,43dvh,264px)] overflow-hidden rounded-[clamp(24px,7dvh,40px)] border-[clamp(4px,1.3dvh,7px)] border-white no-underline shadow-[0_14px_0_var(--color-kid-card-shadow)] ${
                        isNext ? "animate-[breathe_2.4s_ease-in-out_infinite] motion-reduce:animate-none" : ""
                      }`}
                      style={{ background: stripeFor(lesson.lessonId) }}
                    />
                    {done ? <DoneStamp className="pointer-events-none absolute -top-1 -right-3.5" /> : null}
                  </div>
                );
              })}
            </div>

            <div className="flex items-center gap-3 pr-14 pb-[clamp(16px,5dvh,44px)] pl-4" aria-hidden="true">
              {lessons.map((lesson, i) => {
                const done = earned.has(lesson.lessonId);
                const isNext = i === nextIndex;
                return (
                  <span
                    key={lesson.lessonVersionId}
                    className={`h-3.5 rounded-full ${isNext ? "bg-kid-sun w-11.5" : done ? "bg-kid-teal w-3.5" : "bg-kid-pip w-3.5"}`}
                  />
                );
              })}
            </div>
          </>
        ) : null}
      </section>
    </KidStage>
  );
}
