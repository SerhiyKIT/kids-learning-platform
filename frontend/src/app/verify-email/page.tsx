"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import { apiFetch, ensureCsrfCookie } from "@/lib/api";
import { AppHeader } from "@/components/ui/AppHeader";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { AuthLayout, Card, CardBody, CardFooter, PageHeading } from "@/components/ui/Card";

export default function VerifyEmailPage() {
  // useSearchParams() needs a Suspense boundary to keep the rest of the route prerenderable.
  return (
    <Suspense fallback={<AppHeader />}>
      <VerifyEmailScreen />
    </Suspense>
  );
}

/**
 * Landing page of the verification email. Confirms on a click, not on page load: mail scanners
 * that open links in advance must not spend the one-time token. The token is sent only to
 * POST /api/auth/verify-email and dropped from the address bar once used.
 */
function VerifyEmailScreen() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [token] = useState(() => searchParams.get("token") ?? "");
  const [state, setState] = useState<"idle" | "sending" | "done" | "failed">("idle");

  useEffect(() => {
    ensureCsrfCookie();
  }, []);

  async function onConfirm() {
    setState("sending");
    try {
      await apiFetch("/auth/verify-email", { method: "POST", body: JSON.stringify({ token }) });
      setState("done");
      router.replace("/verify-email");
    } catch {
      setState("failed");
    }
  }

  if (state === "done") {
    return (
      <>
        <AppHeader />
        <AuthLayout>
          <PageHeading title="Пошту підтверджено" description="Тепер можна увійти й додати профіль дитини." />
          <Card>
            <CardFooter>
              <Link href="/login" className="contents">
                <Button type="button">Увійти</Button>
              </Link>
            </CardFooter>
          </Card>
        </AuthLayout>
      </>
    );
  }

  return (
    <>
      <AppHeader />
      <AuthLayout>
        <PageHeading title="Підтвердження пошти" description="Натисніть кнопку, щоб підтвердити адресу вашого акаунта." />
        <Card>
          <CardBody>
            {token === "" || state === "failed" ? (
              <Alert title="Посилання недійсне">
                Посилання застаріло або вже використане. Увійдіть і надішліть лист підтвердження ще раз.
              </Alert>
            ) : null}
          </CardBody>
          <CardFooter>
            <Button type="button" onClick={() => void onConfirm()} disabled={token === "" || state === "sending"}>
              {state === "sending" ? "Підтверджуємо…" : "Підтвердити адресу"}
            </Button>
          </CardFooter>
        </Card>
      </AuthLayout>
    </>
  );
}
