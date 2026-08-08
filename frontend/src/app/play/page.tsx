"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { apiFetch, ApiError } from "@/lib/api";

interface Child {
  id: string;
  displayName: string;
  birthYear: number;
  status: string;
}

export default function PlayPage() {
  const [children, setChildren] = useState<Child[] | null>(null);
  const [error, setError] = useState<ApiError | null>(null);

  useEffect(() => {
    apiFetch<Child[]>("/children")
      .then(setChildren)
      .catch((err) => setError(err instanceof ApiError ? err : new ApiError(0, "Unexpected error")));
  }, []);

  return (
    <main className="mx-auto flex max-w-2xl flex-1 flex-col gap-6 p-8">
      <h1 className="text-2xl font-semibold">Хто буде грати?</h1>

      {error ? <p className="text-red-600">{error.message}</p> : null}
      {!children && !error ? <p>Завантаження…</p> : null}
      {children && children.length === 0 ? <p>Спершу додайте дитину в кабінеті.</p> : null}

      <div className="grid grid-cols-2 gap-6 sm:grid-cols-3">
        {children?.map((child) => (
          <Link
            key={child.id}
            href={`/play/${child.id}`}
            className="flex aspect-square min-h-32 flex-col items-center justify-center gap-3 rounded-3xl border-4 border-black/10 bg-white p-4 text-center shadow-md active:scale-95"
          >
            <span className="text-5xl">🧒</span>
            <span className="text-xl font-semibold text-slate-800">{child.displayName}</span>
          </Link>
        ))}
      </div>
    </main>
  );
}
