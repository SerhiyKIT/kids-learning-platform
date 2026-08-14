"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { apiFetch, ApiError, ensureCsrfCookie } from "@/lib/api";
import type { DevRegisterRoleRequest, DevRegisterRoleResponse, Role } from "@/lib/api-types";

interface RegisterResponse {
  id: string;
  email: string;
  displayName: string;
}

const IS_DEV = process.env.NODE_ENV !== "production";

const DEV_ROLE_LABELS: Record<Role, string> = {
  PARENT: "Батьки",
  TEACHER: "Вчитель",
  ADMIN: "Адміністратор",
};

export default function RegisterPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [devRole, setDevRole] = useState<Role>("PARENT");
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
      if (IS_DEV && devRole !== "PARENT") {
        // Dev-only shortcut: the only way to obtain a TEACHER/ADMIN login, since the normal
        // endpoint below always creates a PARENT. The account comes back already verified, so
        // there's no "check your email" step — straight to login.
        const body: DevRegisterRoleRequest = { email, password, displayName, role: devRole };
        await apiFetch<DevRegisterRoleResponse>("/dev/register-role", {
          method: "POST",
          body: JSON.stringify(body),
        });
        router.push("/login");
        return;
      }
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

        {IS_DEV ? (
          <label className="flex flex-col gap-1 rounded border border-dashed border-amber-500 p-2">
            <span className="text-amber-700 dark:text-amber-500">Роль (лише для розробки)</span>
            <select
              value={devRole}
              onChange={(e) => setDevRole(e.target.value as Role)}
              className="rounded border border-black/[.16] px-3 py-2 dark:border-white/[.2]"
            >
              {(Object.entries(DEV_ROLE_LABELS) as [Role, string][]).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </label>
        ) : null}

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
  if (error.status === 409) {
    return <p className="text-red-600">This email is already registered.</p>;
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
