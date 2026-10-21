"use client";

import { useState } from "react";
import { QuietButton } from "./SecondaryButton";

/** Копіює `text` у буфер обміну на клік і коротко підтверджує це в підписі кнопки. */
export function CopyButton({ text, label = "Копіювати" }: { text: string; label?: string }) {
  const [state, setState] = useState<"idle" | "copied" | "failed">("idle");

  async function onCopy() {
    try {
      await navigator.clipboard.writeText(text);
      setState("copied");
    } catch {
      setState("failed");
    }
    setTimeout(() => setState("idle"), 2000);
  }

  return (
    <QuietButton type="button" onClick={onCopy} aria-live="polite">
      {state === "copied" ? "Скопійовано" : state === "failed" ? "Не вдалося скопіювати" : label}
    </QuietButton>
  );
}
