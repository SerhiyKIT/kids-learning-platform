"use client";

import { useId, useState, type ReactNode } from "react";

const inputBase =
  "bg-surface text-ink h-[46px] w-full rounded-lg border px-3.5 text-base";

export function Field({
  label,
  hint,
  error,
  children,
}: {
  label: string;
  hint?: string;
  error?: string;
  children: (ids: { id: string; describedBy?: string }) => ReactNode;
}) {
  const id = useId();
  const describedBy = error || hint ? `${id}-note` : undefined;
  return (
    <div className="flex flex-col gap-[7px]">
      <label
        htmlFor={id}
        className={`text-[15px] font-medium ${error ? "text-danger" : "text-ink"}`}
      >
        {label}
      </label>
      {children({ id, describedBy })}
      {error ? (
        <p id={describedBy} className="text-danger flex items-center gap-[7px] text-[13px] leading-snug">
          <ExclamationDot />
          <span>{error}</span>
        </p>
      ) : hint ? (
        <p id={describedBy} className="text-ink-soft text-[13px] leading-snug">
          {hint}
        </p>
      ) : null}
    </div>
  );
}

export function TextInput({
  invalid,
  className = "",
  ...props
}: React.ComponentPropsWithoutRef<"input"> & { invalid?: boolean }) {
  return (
    <input
      {...props}
      aria-invalid={invalid || undefined}
      className={`${inputBase} ${invalid ? "border-danger-field" : "border-line-field"} ${className}`}
    />
  );
}

/** Поле пароля з перемикачем видимості (кнопка накладена справа). */
export function PasswordInput({
  invalid,
  ...props
}: React.ComponentPropsWithoutRef<"input"> & { invalid?: boolean }) {
  const [visible, setVisible] = useState(false);
  return (
    <div className="relative flex items-center">
      <input
        {...props}
        type={visible ? "text" : "password"}
        aria-invalid={invalid || undefined}
        className={`${inputBase} pr-[46px] ${invalid ? "border-danger-field" : "border-line-field"}`}
      />
      <button
        type="button"
        onClick={() => setVisible((v) => !v)}
        aria-label={visible ? "Сховати пароль" : "Показати пароль"}
        aria-pressed={visible}
        className="text-ink-icon hover:bg-hover-soft hover:text-ink absolute right-1.5 flex size-[34px] cursor-pointer items-center justify-center rounded-lg"
      >
        <svg width="18" height="18" viewBox="0 0 20 20" fill="none" aria-hidden="true">
          <ellipse cx="10" cy="10" rx="8" ry="5" stroke="currentColor" strokeWidth="1.4" />
          <circle cx="10" cy="10" r="2.1" fill="currentColor" />
        </svg>
      </button>
    </div>
  );
}

export function Select({
  className = "",
  ...props
}: React.ComponentPropsWithoutRef<"select">) {
  return (
    <div className="relative flex items-center">
      <select
        {...props}
        className={`bg-surface text-ink h-11 w-full appearance-none rounded-lg border pr-9.5 pl-3.25 text-base ${className}`}
      />
      <span className="pointer-events-none absolute right-3.5 size-2 -translate-y-0.5 rotate-45 border-r-[1.6px] border-b-[1.6px] border-[color:var(--color-ink-icon)]" />
    </div>
  );
}

export function ExclamationDot() {
  return (
    <span
      aria-hidden="true"
      className="border-danger flex size-[15px] flex-none items-center justify-center rounded-full border text-[10px] font-semibold"
    >
      !
    </span>
  );
}
