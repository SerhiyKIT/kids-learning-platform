"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { apiFetch } from "@/lib/api";
import type { Sticker, StickersResponse } from "@/lib/api-types";
import { ExitButton, KidStage, ListenButton, Mascot } from "@/components/kid/KidChrome";
import { StickerArt, TopicIcon } from "@/components/kid/Stickers";
import { useKidMode } from "@/components/kid/kid-mode-context";
import { useKidAudio, useSpeakingUrl } from "@/components/scene-engine/KidAudio";

interface TopicRow {
  topic: string;
  completed: boolean;
  stickers: Sticker[];
  locked: string[];
}

function byTopic(album: StickersResponse): TopicRow[] {
  const rows = new Map<string, TopicRow>();
  const row = (topic: string) => {
    let r = rows.get(topic);
    if (!r) {
      r = { topic, completed: album.topicsCompleted.includes(topic), stickers: [], locked: [] };
      rows.set(topic, r);
    }
    return r;
  };
  album.stickers.forEach((s) => row(s.topic).stickers.push(s));
  album.locked.forEach((l) => row(l.topic).locked.push(l.lessonId));
  return [...rows.values()];
}

/**
 * The sticker album (docs/UX_гайд_дитячого_режиму.md §7): one row per topic, the topic sticker first
 * (lit once every lesson of the topic is passed), then earned stickers and grey silhouettes for the
 * rest — never a count of what's missing, never a ranking. Tapping a sticker makes the guide say
 * that lesson's rule again, so the album doubles as revision.
 */
export default function StickerAlbumPage() {
  const { childId } = useParams<{ childId: string }>();
  const { line } = useKidMode();
  const { play, replay } = useKidAudio();
  const speakingUrl = useSpeakingUrl();
  const [rows, setRows] = useState<TopicRow[] | null>(null);
  const greeted = useRef(false);
  const introUrl = line("album_intro")?.audioUrl;

  useEffect(() => {
    apiFetch<StickersResponse>(`/children/${childId}/stickers`)
      .then((album) => setRows(byTopic(album)))
      .catch(() => setRows([]));
  }, [childId]);

  useEffect(() => {
    if (introUrl && !greeted.current) {
      greeted.current = true;
      void play(introUrl);
    }
  }, [introUrl, play]);

  const onGuideTap = () => (introUrl ? void play(introUrl, { restart: true }) : replay());

  return (
    <KidStage>
      <ExitButton />

      <aside className="relative z-2 flex h-full flex-none flex-col items-center justify-center gap-[clamp(8px,3dvh,24px)] py-[clamp(16px,5dvh,40px)] pr-2 pl-[clamp(20px,3vw,32px)]">
        <button type="button" onClick={onGuideTap} aria-label="Провідник: пояснити ще раз" className="cursor-pointer">
          <Mascot speaking={speakingUrl !== null} className="h-[clamp(84px,30dvh,240px)] w-[clamp(70px,25dvh,200px)]" />
        </button>
        <div className="flex items-center gap-[clamp(10px,2.5dvh,20px)] [@media(min-height:560px)]:flex-col">
          <ListenButton label="Послухати ще раз" onClick={onGuideTap} speaking={speakingUrl !== null} className="size-[clamp(68px,16dvh,96px)]" />
          <Link
            href={`/play/${childId}`}
            aria-label="До уроків"
            className="bg-kid-sun text-kid-sun-deep flex size-[clamp(68px,16dvh,92px)] items-center justify-center rounded-[28%] border-[clamp(4px,1dvh,6px)] border-white no-underline shadow-[0_10px_0_var(--color-kid-sun-shadow)]"
          >
            <svg viewBox="0 0 24 24" fill="none" className="size-[56%]" aria-hidden="true">
              <path d="M4 11.2 12 4.5l8 6.7V19a1.5 1.5 0 0 1-1.5 1.5H15v-5.5H9v5.5H5.5A1.5 1.5 0 0 1 4 19v-7.8Z" fill="currentColor" />
            </svg>
          </Link>
        </div>
      </aside>

      <section
        aria-label="Мої наліпки"
        className="relative z-2 flex h-full min-w-0 flex-1 flex-col justify-center gap-[clamp(10px,4dvh,32px)] overflow-y-auto py-[clamp(22px,5dvh,40px)] pr-[clamp(56px,7vw,80px)] pl-4"
      >
        {rows?.map((row) => (
          <div
            key={row.topic}
            data-topic={row.topic}
            className="flex flex-none items-center gap-[clamp(10px,2.5vw,22px)] overflow-x-auto rounded-[clamp(22px,6dvh,40px)] bg-white/45 px-[clamp(12px,2.5vw,24px)] py-[clamp(10px,2.5dvh,18px)]"
          >
            <span
              role="img"
              aria-label={row.completed ? "Наліпка теми здобута" : "Наліпка теми ще попереду"}
              data-topic-done={row.completed ? "1" : "0"}
              className={`flex size-[clamp(72px,24dvh,132px)] flex-none items-center justify-center rounded-[30%] border-[clamp(4px,1.2dvh,7px)] ${
                row.completed
                  ? "bg-kid-sun border-white shadow-[0_10px_0_var(--color-kid-sun-shadow)]"
                  : "border-kid-pip bg-kid-pip"
              }`}
            >
              <TopicIcon topic={row.topic} className={`size-[64%] ${row.completed ? "" : "opacity-40 grayscale"}`} />
            </span>

            {row.stickers.map((sticker) => {
              const url = sticker.bridge.audioUrl;
              const speaking = url !== null && speakingUrl === url;
              return (
                <button
                  key={sticker.lessonId}
                  type="button"
                  aria-label={`Наліпка «${sticker.title}»: послухати правило`}
                  data-sticker={sticker.lessonId}
                  onClick={() => (url ? void play(url, { restart: true }) : undefined)}
                  className={`flex-none cursor-pointer rounded-full ${
                    speaking ? "animate-[talk_0.45s_ease-in-out_infinite] motion-reduce:animate-none" : ""
                  }`}
                >
                  <StickerArt seed={sticker.lessonId} topic={sticker.topic} className="size-[clamp(68px,22dvh,124px)]" />
                </button>
              );
            })}

            {row.locked.map((lessonId) => (
              <span key={lessonId} data-locked={lessonId} role="img" aria-label="Наліпка ще попереду" className="flex-none">
                <StickerArt seed={lessonId} topic={row.topic} locked className="size-[clamp(68px,22dvh,124px)]" />
              </span>
            ))}
          </div>
        ))}
      </section>
    </KidStage>
  );
}
