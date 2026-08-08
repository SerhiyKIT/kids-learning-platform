"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { apiFetch, ApiError, logout } from "@/lib/api";

interface MeResponse {
  id: string;
  email: string;
  role: "PARENT" | "TEACHER" | "ADMIN";
  emailVerified?: boolean;
}

export default function DashboardPage() {
  const router = useRouter();
  const [me, setMe] = useState<MeResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    apiFetch<MeResponse>("/auth/me")
      .then((data) => {
        if (!cancelled) setMe(data);
      })
      .catch((err) => {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 401) {
          router.replace("/login");
          return;
        }
        setMe(null);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [router]);

  async function onLogout() {
    await logout();
    router.push("/login");
  }

  if (loading) {
    return (
      <main className="flex flex-1 items-center justify-center p-8">
        <p>Loading…</p>
      </main>
    );
  }

  if (!me) {
    return (
      <main className="flex flex-1 items-center justify-center p-8">
        <p>Could not load your account.</p>
      </main>
    );
  }

  return (
    <main className="mx-auto flex max-w-sm flex-1 flex-col justify-center gap-4 p-8">
      <h1 className="text-xl font-semibold">Dashboard</h1>
      <dl className="flex flex-col gap-2">
        <div>
          <dt className="text-sm text-black/60 dark:text-white/60">Email</dt>
          <dd>{me.email}</dd>
        </div>
        <div>
          <dt className="text-sm text-black/60 dark:text-white/60">Role</dt>
          <dd>{me.role}</dd>
        </div>
        {me.emailVerified !== undefined ? (
          <div>
            <dt className="text-sm text-black/60 dark:text-white/60">Email verified</dt>
            <dd>{me.emailVerified ? "Yes" : "No"}</dd>
          </div>
        ) : null}
      </dl>
      <button
        type="button"
        onClick={onLogout}
        className="mt-2 self-start rounded-full border border-black/[.16] px-5 py-2 dark:border-white/[.2]"
      >
        Log out
      </button>
    </main>
  );
}
