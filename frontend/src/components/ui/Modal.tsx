"use client";

import { useEffect, type ReactNode } from "react";

/** Модальне підтвердження. Esc закриває, фокус лишається в межах картки. */
export function Modal({
  title,
  onClose,
  children,
  footer,
}: {
  title: string;
  onClose: () => void;
  children: ReactNode;
  footer: ReactNode;
}) {
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [onClose]);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[color:var(--color-ink)]/45 p-4">
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className="bg-surface border-line flex w-full max-w-[440px] flex-col gap-4 rounded-xl border p-6 shadow-[0_18px_44px_rgba(18,40,61,0.22)]"
      >
        <h3 className="text-[20px] font-medium tracking-[-0.01em]">{title}</h3>
        {children}
        <div className="flex justify-end gap-2.5">{footer}</div>
      </div>
    </div>
  );
}
