"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { apiFetch, ApiError } from "@/lib/api";

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
  const [error, setError] = useState<ApiError | null>(null);

  useEffect(() => {
    apiFetch<AvailableLesson[]>(`/children/${childId}/available-lessons`)
      .then(setLessons)
      .catch((err) => setError(err instanceof ApiError ? err : new ApiError(0, "Unexpected error")));
  }, [childId]);

  return (
    <main className="mx-auto flex max-w-3xl flex-1 flex-col gap-6 p-8">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Обери урок</h1>
        <Link href="/play" className="text-blue-600 underline">
          ← Інша дитина
        </Link>
      </div>

      {error ? <p className="text-red-600">{error.message}</p> : null}
      {!lessons && !error ? <p>Завантаження…</p> : null}
      {lessons && lessons.length === 0 ? <p>Поки що немає призначених уроків.</p> : null}

      <div className="grid grid-cols-2 gap-6 sm:grid-cols-3">
        {lessons?.map((lesson) => (
          <Link
            key={lesson.lessonVersionId}
            href={`/play/${childId}/${lesson.lessonVersionId}`}
            className="flex aspect-square min-h-32 flex-col items-center justify-center gap-3 rounded-3xl border-4 border-black/10 bg-white p-4 text-center shadow-md active:scale-95"
          >
            <span className="text-5xl">📘</span>
            <span className="text-xl font-semibold text-slate-800">{lesson.title}</span>
          </Link>
        ))}
      </div>
    </main>
  );
}
