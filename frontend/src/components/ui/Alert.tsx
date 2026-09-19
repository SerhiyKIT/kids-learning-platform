import type { ReactNode } from "react";
import { ExclamationDot } from "./Field";

/** Блок помилки над полями форми. */
export function Alert({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div
      role="alert"
      className="bg-danger-surface border-danger-line flex items-start gap-[11px] rounded-lg border px-4 py-3.5"
    >
      <span className="border-danger text-danger mt-px flex size-[18px] flex-none items-center justify-center rounded-full border text-[11px] font-semibold">
        !
      </span>
      <div className="flex flex-col gap-0.75">
        <p className="text-danger-title text-[15px] font-medium">{title}</p>
        {children ? (
          <div className="text-danger-body text-sm leading-relaxed text-pretty">{children}</div>
        ) : null}
      </div>
    </div>
  );
}

export { ExclamationDot };
