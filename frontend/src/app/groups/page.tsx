"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { apiFetch, ApiError } from "@/lib/api";
import type { CreateGroupRequest, Group } from "@/lib/api-types";

const STATUS_BADGE = {
  active: { label: "Активна", className: "bg-emerald-100 text-emerald-800" },
  archived: { label: "Архів", className: "bg-black/10 text-black/60 dark:bg-white/10 dark:text-white/60" },
};

export default function GroupsPage() {
  const [groups, setGroups] = useState<Group[] | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  const [copiedId, setCopiedId] = useState<string | null>(null);

  const [creating, setCreating] = useState(false);
  const [newName, setNewName] = useState("");
  const [createError, setCreateError] = useState<ApiError | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(() => {
    apiFetch<Group[]>("/groups")
      .then(setGroups)
      .catch((err) => setError(err instanceof ApiError ? err : new ApiError(0, "Unexpected error")));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function onCreate(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setCreateError(null);
    try {
      const body: CreateGroupRequest = { name: newName };
      await apiFetch<Group>("/groups", { method: "POST", body: JSON.stringify(body) });
      setNewName("");
      setCreating(false);
      load();
    } catch (err) {
      setCreateError(err instanceof ApiError ? err : new ApiError(0, "Unexpected error"));
    } finally {
      setSubmitting(false);
    }
  }

  async function onCopy(group: Group) {
    try {
      await navigator.clipboard.writeText(group.joinCode);
      setCopiedId(group.id);
      setTimeout(() => setCopiedId((current) => (current === group.id ? null : current)), 2000);
    } catch {
      // Clipboard API can fail (permissions, insecure context) — the code is still visible on
      // the card, so there's nothing more useful to do than silently leave the button as-is.
    }
  }

  return (
    <main className="mx-auto flex w-full max-w-2xl flex-1 flex-col gap-6 p-6 sm:p-8">
      <div className="flex items-center justify-between">
        <div>
          <Link href="/dashboard" className="text-sm text-blue-600 underline">
            ← До кабінету
          </Link>
          <h1 className="mt-2 text-2xl font-semibold">Мої групи</h1>
        </div>
        <button
          type="button"
          onClick={() => setCreating((c) => !c)}
          className="shrink-0 rounded-full bg-blue-600 px-4 py-2 text-sm text-white"
        >
          Створити групу
        </button>
      </div>

      {creating ? (
        <form onSubmit={onCreate} className="flex items-end gap-2 rounded-2xl border border-black/10 p-4 dark:border-white/10">
          <label className="flex flex-1 flex-col gap-1">
            <span className="text-sm">Назва групи</span>
            <input
              type="text"
              required
              autoFocus
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              className="rounded border border-black/[.16] px-3 py-2 dark:border-white/[.2]"
            />
          </label>
          <button
            type="submit"
            disabled={submitting}
            className="rounded-full bg-foreground px-4 py-2 text-background disabled:opacity-50"
          >
            {submitting ? "Створюємо…" : "Створити"}
          </button>
        </form>
      ) : null}
      {createError ? <p className="text-red-600">{createError.message}</p> : null}

      {error ? <p className="text-red-600">{error.message}</p> : null}
      {!groups && !error ? <p>Завантаження…</p> : null}
      {groups && groups.length === 0 ? (
        <p className="text-black/60 dark:text-white/60">Ще немає жодної групи.</p>
      ) : null}

      {groups && groups.length > 0 ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          {groups.map((group) => {
            const badge = STATUS_BADGE[group.isActive ? "active" : "archived"];
            return (
              <Link
                key={group.id}
                href={`/groups/${group.id}`}
                className="flex flex-col gap-2 rounded-2xl border border-black/10 bg-white p-4 shadow-sm dark:border-white/10 dark:bg-white/5"
              >
                <div className="flex items-center justify-between">
                  <span className="font-medium text-slate-800 dark:text-white">{group.name}</span>
                  <span className={`rounded-full px-2 py-0.5 text-xs ${badge.className}`}>{badge.label}</span>
                </div>
                <div className="flex items-center justify-between gap-2 text-sm">
                  <span className="font-mono text-black/70 dark:text-white/70">{group.joinCode}</span>
                  <button
                    type="button"
                    onClick={(e) => {
                      e.preventDefault();
                      onCopy(group);
                    }}
                    className="rounded-full border border-black/[.16] px-3 py-1 text-xs dark:border-white/[.2]"
                  >
                    {copiedId === group.id ? "Скопійовано" : "Копіювати код"}
                  </button>
                </div>
              </Link>
            );
          })}
        </div>
      ) : null}
    </main>
  );
}
