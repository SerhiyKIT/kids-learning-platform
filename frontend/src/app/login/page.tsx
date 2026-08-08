"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { ApiError, ensureCsrfCookie, loginWithFormPost } from "@/lib/api";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<ApiError | null>(null);

  useEffect(() => {
    ensureCsrfCookie();
  }, []);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await loginWithFormPost(email, password);
      router.push("/dashboard");
    } catch (err) {
      setError(err instanceof ApiError ? err : new ApiError(0, "Unexpected error"));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="mx-auto flex max-w-sm flex-1 flex-col justify-center gap-4 p-8">
      <h1 className="text-xl font-semibold">Log in</h1>
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
          <span>Password</span>
          <input
            type="password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="rounded border border-black/[.16] px-3 py-2 dark:border-white/[.2]"
          />
        </label>

        {error ? <ErrorMessage error={error} /> : null}

        <button
          type="submit"
          disabled={submitting}
          className="mt-2 rounded-full bg-foreground px-5 py-2 text-background disabled:opacity-50"
        >
          {submitting ? "Logging in…" : "Log in"}
        </button>
      </form>
      <p>
        No account yet?{" "}
        <Link href="/register" className="underline">
          Register
        </Link>
      </p>
    </main>
  );
}

function ErrorMessage({ error }: { error: ApiError }) {
  if (error.status === 429) {
    return (
      <p className="text-red-600">
        Too many attempts.{" "}
        {error.retryAfterSeconds ? `Try again in ${error.retryAfterSeconds}s.` : "Try again later."}
      </p>
    );
  }
  if (error.status === 401) {
    return <p className="text-red-600">Invalid email or password.</p>;
  }
  return <p className="text-red-600">{error.message}</p>;
}
