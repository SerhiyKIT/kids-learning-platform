"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { apiFetch, ApiError } from "@/lib/api";

interface Child {
  id: string;
  displayName: string;
  birthYear: number;
  status: string;
}

const IS_DEV = process.env.NODE_ENV !== "production";

export default function PlayPage() {
  const [children, setChildren] = useState<Child[] | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  const [seeding, setSeeding] = useState(false);
  const [seedError, setSeedError] = useState<ApiError | null>(null);

  const loadChildren = useCallback(() => {
    apiFetch<Child[]>("/children")
      .then(setChildren)
      .catch((err) => setError(err instanceof ApiError ? err : new ApiError(0, "Unexpected error")));
  }, []);

  useEffect(() => {
    loadChildren();
  }, [loadChildren]);

  async function onSeedDemo() {
    setSeeding(true);
    setSeedError(null);
    try {
      await apiFetch("/dev/seed-demo", { method: "POST" });
      loadChildren();
    } catch (err) {
      setSeedError(err instanceof ApiError ? err : new ApiError(0, "Unexpected error"));
    } finally {
      setSeeding(false);
    }
  }

  return (
    <main className="mx-auto flex max-w-2xl flex-1 flex-col gap-6 p-8">
      <h1 className="text-2xl font-semibold">Хто буде грати?</h1>

      {error ? <p className="text-red-600">{error.message}</p> : null}
      {!children && !error ? <p>Завантаження…</p> : null}
      {children && children.length === 0 ? <p>Спершу додайте дитину в кабінеті.</p> : null}

      {children && children.length === 0 && IS_DEV ? (
        <div className="flex flex-col gap-2">
          <button
            type="button"
            onClick={onSeedDemo}
            disabled={seeding}
            className="self-start rounded-full bg-emerald-600 px-5 py-2 text-white disabled:opacity-60"
          >
            {seeding ? "Створюємо…" : "Створити демо-урок"}
          </button>
          {seedError ? <p className="text-red-600">{seedError.message}</p> : null}
        </div>
      ) : null}

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
