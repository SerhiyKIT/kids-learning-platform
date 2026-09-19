"use client";

import { useParams } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import { apiFetch, ApiError } from "@/lib/api";
import type { HistoryEntry } from "@/lib/api-types";
import {
  DoneStamp,
  ExitButton,
  KidStage,
  ListenButton,
  Mascot,
  stripeFor,
} from "@/components/kid/KidChrome";

interface AvailableLesson {
  lessonVersionId: string;
  lessonId: string;
  title: string;
  moduleCode: string;
  assignmentId: string;
  dueAt: string | null;
}

export default function ChildLessonsPage() {
  const { childId } = useParams<{ childId: string }>();
  const [lessons, setLessons] = useState<AvailableLesson[] | null>(null);
  const [history, setHistory] = useState<HistoryEntry[] | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  const shelfRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    apiFetch<AvailableLesson[]>(`/children/${childId}/available-lessons`)
      .then(setLessons)
      .catch((err) => setError(err instanceof ApiError ? err : new ApiError(0, "Несподівана помилка")));
    // Історія лише розмальовує картки печаткою «пройдено» — її помилка не має ламати полицю.
    apiFetch<HistoryEntry[]>(`/children/${childId}/history`)
      .then(setHistory)
      .catch(() => setHistory([]));
  }, [childId]);

  // Дитина не має шукати, з чого продовжити — наступний урок сам стає в центр полиці.
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

  const completedTitles = new Set(
    (history ?? []).filter((h) => h.result === "completed").map((h) => h.title),
  );
  const nextIndex = lessons?.findIndex((l) => !completedTitles.has(l.title)) ?? -1;

  return (
    <KidStage>
      <ExitButton />

      <aside className="relative z-2 flex h-full w-61 flex-none flex-col items-center justify-center gap-6.5 px-7 py-10">
        <Mascot />
        <ListenButton />
      </aside>

      <section className="relative z-2 flex h-full min-w-0 flex-1 flex-col justify-center">
        {error ? (
          <p className="text-kid-ink px-14 text-2xl font-semibold">
            Уроки не завантажились. Покажи це дорослому.
          </p>
        ) : null}
        {!lessons && !error ? (
          <p className="text-kid-ink-soft px-14 text-2xl font-semibold">Готуємо уроки…</p>
        ) : null}
        {lessons && lessons.length === 0 ? (
          <p className="text-kid-ink px-14 text-2xl font-semibold">
            Поки що немає уроків. Скоро з&apos;являться!
          </p>
        ) : null}

        {lessons && lessons.length > 0 ? (
          <>
            <div
              ref={shelfRef}
              className="kid-shelf flex snap-x snap-mandatory items-center gap-6 overflow-x-auto overflow-y-hidden pt-10 pr-14 pb-8.5 pl-2 [mask-image:linear-gradient(to_right,transparent_0,#000_90px,#000_calc(100%-130px),transparent_100%)]"
            >
              {lessons.map((lesson, i) => {
                const done = completedTitles.has(lesson.title);
                const isNext = i === nextIndex;
                return (
                  <div
                    key={lesson.lessonVersionId}
                    data-next={isNext ? "1" : "0"}
                    className="relative flex-none snap-center pt-2.5"
                  >
                    {isNext ? (
                      <span
                        aria-hidden="true"
                        className="bg-kid-glow absolute inset-x-1.5 -top-3.5 bottom-1.5 animate-[glowring_2.4s_ease-in-out_infinite] rounded-[44px]"
                      />
                    ) : null}

                    <a
                      href={`/play/${childId}/${lesson.lessonVersionId}`}
                      aria-label={done ? `${lesson.title} — пройдено` : `Грати урок ${lesson.title}`}
                      className={`relative flex h-86 w-66 flex-col overflow-hidden rounded-[40px] border-7 border-white bg-white no-underline shadow-[0_14px_0_var(--color-kid-card-shadow)] ${
                        isNext ? "animate-[breathe_2.4s_ease-in-out_infinite]" : ""
                      }`}
                    >
                      <span
                        className="flex flex-1 items-center justify-center"
                        style={{ background: stripeFor(lesson.lessonId) }}
                        role="img"
                        aria-label={`Ілюстрація уроку «${lesson.title}»`}
                      />
                      <span className="text-kid-ink flex h-21 flex-none items-center justify-center px-4 text-center text-[22px] leading-tight font-semibold">
                        {lesson.title}
                      </span>
                    </a>

                    {done ? <DoneStamp className="absolute -top-1 -right-3.5" /> : null}
                  </div>
                );
              })}
            </div>

            <div className="flex items-center gap-3 pr-14 pb-11 pl-2">
              {lessons.map((lesson, i) => {
                const done = completedTitles.has(lesson.title);
                const isNext = i === nextIndex;
                return (
                  <span
                    key={lesson.lessonVersionId}
                    className={`h-3.5 rounded-full ${
                      isNext ? "bg-kid-sun w-11.5" : done ? "bg-kid-teal w-3.5" : "bg-kid-pip w-3.5"
                    }`}
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
