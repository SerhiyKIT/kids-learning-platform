"use client";

import { useRef, type ReactNode } from "react";
import { createPortal } from "react-dom";
import { useDialogFocus } from "./use-dialog-focus";

/**
 * Модальне підтвердження: рендериться в <body>, фокус усередині й циклічний Tab, фон inert, Esc
 * закриває, після закриття фокус повертається на кнопку, що відкрила (useDialogFocus).
 */
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
  const dialogRef = useRef<HTMLDivElement>(null);
  useDialogFocus(dialogRef, onClose);

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[color:var(--color-ink)]/45 p-4">
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-label={title}
        tabIndex={-1}
        className="bg-surface border-line flex w-full max-w-[440px] flex-col gap-4 rounded-xl border p-6 shadow-[0_18px_44px_rgba(18,40,61,0.22)] outline-none"
      >
        <h3 className="text-[20px] font-medium tracking-[-0.01em]">{title}</h3>
        {children}
        <div className="flex justify-end gap-2.5">{footer}</div>
      </div>
    </div>,
    document.body,
  );
}
