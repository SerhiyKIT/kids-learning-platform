"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { apiFetch, ApiError, ensureCsrfCookie } from "@/lib/api";

interface RegisterResponse {
  id: string;
  email: string;
  displayName: string;
}

export default function RegisterPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<ApiError | null>(null);
  const [registered, setRegistered] = useState(false);

  useEffect(() => {
    ensureCsrfCookie();
  }, []);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await apiFetch<RegisterResponse>("/auth/register", {
        method: "POST",
        body: JSON.stringify({ email, password, displayName }),
      });
      setRegistered(true);
    } catch (err) {
      setError(err instanceof ApiError ? err : new ApiError(0, "Unexpected error"));
    } finally {
      setSubmitting(false);
    }
  }

  if (registered) {
    return (
      <main className="mx-auto flex max-w-sm flex-1 flex-col justify-center gap-4 p-8">
        <h1 className="text-xl font-semibold">Check your email</h1>
        <p>We sent a verification link to {email}. Confirm it before logging in.</p>
        <Link href="/login" className="underline">
          Go to login
        </Link>
      </main>
    );
  }

  return (
    <main className="mx-auto flex max-w-sm flex-1 flex-col justify-center gap-4 p-8">
      <h1 className="text-xl font-semibold">Register</h1>
      <form onSubmit={onSubmit} className="flex flex-col gap-3">
        <label className="flex flex-col gap-1">
          <span>Email</span>
          <input
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="rounded border border-black/[.16] px-3 py-2 dark:border-white/[.2]"
          />
        </label>
        <label className="flex flex-col gap-1">
          <span>Password (min 10 characters)</span>
          <input
            type="password"
            required
            minLength={10}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="rounded border border-black/[.16] px-3 py-2 dark:border-white/[.2]"
          />
        </label>
        <label className="flex flex-col gap-1">
          <span>Display name</span>
          <input
            type="text"
            required
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
            className="rounded border border-black/[.16] px-3 py-2 dark:border-white/[.2]"
          />
        </label>

        {error ? <ErrorMessage error={error} /> : null}

        <button
          type="submit"
          disabled={submitting}
          className="mt-2 rounded-full bg-foreground px-5 py-2 text-background disabled:opacity-50"
        >
          {submitting ? "Registering…" : "Register"}
        </button>
      </form>
      <p>
        Already have an account?{" "}
        <Link href="/login" className="underline">
          Log in
        </Link>
      </p>
    </main>
  );
}

function ErrorMessage({ error }: { error: ApiError }) {
  if (error.status === 429) {
    return (
      <p className="text-red-600">
        Too many attempts. {error.retryAfterSeconds ? `Try again in ${error.retryAfterSeconds}s.` : "Try again later."}
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
