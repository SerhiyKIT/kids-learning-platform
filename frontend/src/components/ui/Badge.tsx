import type { ReactNode } from "react";

/** Капсула-маркер: моно, капс, розріджений трекінг. */
export function Badge({ children }: { children: ReactNode }) {
  return (
    <span className="font-mono text-ink-muted bg-surface border-line-chip rounded-full border px-3.5 py-1.5 text-xs tracking-[0.08em] uppercase">
      {children}
    </span>
  );
}
