import type { ReactNode } from "react";

const tones = {
  ok: "text-ok-ink bg-ok-surface border-ok-line",
  waiting: "text-notice-body bg-notice-surface border-notice-line",
  bad: "text-danger-title bg-danger-surface border-danger-line",
  neutral: "text-ink-muted bg-canvas border-line-chip",
} as const;

/** Статус-пілюля: активна / очікує згоди / архів. */
export function Pill({ tone = "neutral", children }: { tone?: keyof typeof tones; children: ReactNode }) {
  return (
    <span className={`self-start rounded-full border px-2.75 py-0.75 text-[13px] font-medium ${tones[tone]}`}>
      {children}
    </span>
  );
}
