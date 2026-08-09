"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { apiFetch, ApiError } from "@/lib/api";
import type { Child, CreateChildRequest, ParentRelation } from "@/lib/api-types";

const CURRENT_YEAR = new Date().getFullYear();
// 3–6yo range, per ChildService's own bounds (max 8 years old) — just narrower and sane for a
// preschool audience.
const AGE_OPTIONS = [3, 4, 5, 6];

const RELATION_LABELS: Record<ParentRelation, string> = {
  mother: "Мама",
  father: "Тато",
  guardian: "Опікун",
};

export default function NewChildPage() {
  const router = useRouter();
  const [displayName, setDisplayName] = useState("");
  const [age, setAge] = useState(AGE_OPTIONS[2]);
  const [relation, setRelation] = useState<ParentRelation>("mother");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<ApiError | null>(null);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const body: CreateChildRequest = {
        displayName,
        birthYear: CURRENT_YEAR - age,
        relation,
      };
      const child = await apiFetch<Child>("/children", {
        method: "POST",
        body: JSON.stringify(body),
      });
      router.push(`/children/${child.id}`);
    } catch (err) {
      setError(err instanceof ApiError ? err : new ApiError(0, "Unexpected error"));
      setSubmitting(false);
    }
  }

  return (
    <main className="mx-auto flex w-full max-w-sm flex-1 flex-col justify-center gap-4 p-6 sm:p-8">
      <div>
        <Link href="/dashboard" className="text-sm text-blue-600 underline">
          ← До кабінету
        </Link>
        <h1 className="mt-2 text-xl font-semibold">Додати дитину</h1>
      </div>

      <form onSubmit={onSubmit} className="flex flex-col gap-3">
        <label className="flex flex-col gap-1">
          <span>Ім&apos;я дитини</span>
          <input
            type="text"
            required
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
            className="rounded border border-black/[.16] px-3 py-2 dark:border-white/[.2]"
          />
        </label>

        <label className="flex flex-col gap-1">
          <span>Вік</span>
          <select
            value={age}
            onChange={(e) => setAge(Number(e.target.value))}
            className="rounded border border-black/[.16] px-3 py-2 dark:border-white/[.2]"
          >
            {AGE_OPTIONS.map((a) => (
              <option key={a} value={a}>
                {a} років
              </option>
            ))}
          </select>
        </label>

        <label className="flex flex-col gap-1">
          <span>Ким ви є для дитини</span>
          <select
            value={relation}
            onChange={(e) => setRelation(e.target.value as ParentRelation)}
            className="rounded border border-black/[.16] px-3 py-2 dark:border-white/[.2]"
          >
            {Object.entries(RELATION_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </label>

        {error ? <ErrorMessage error={error} /> : null}

        <button
          type="submit"
          disabled={submitting}
          className="mt-2 rounded-full bg-foreground px-5 py-2 text-background disabled:opacity-50"
        >
          {submitting ? "Додаємо…" : "Додати"}
        </button>
      </form>
    </main>
  );
}

function ErrorMessage({ error }: { error: ApiError }) {
  if (error.status === 403 && error.code === "EMAIL_NOT_VERIFIED") {
    return (
      <p className="text-red-600">
        Спершу підтвердьте email — перейдіть до кабінету, там є кнопка повторної відправки листа.
      </p>
    );
  }
  if (error.problems && error.problems.length > 0) {
    return (
      <ul className="text-red-600">
        {error.problems.map((p, i) => (
          <li key={i}>
            {p.path}: {p.message}
          </li>
        ))}
      </ul>
    );
  }
  return <p className="text-red-600">{error.message}</p>;
}
