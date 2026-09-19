"use client";

import { useId, useState, type ReactNode } from "react";

/**
 * Підказка над недоступною дією. Обгортка сама віддає дитині id підказки, щоб
 * disabled-кнопка могла посилатися на неї через aria-describedby.
 */
export function Tooltip({
  text,
  children,
}: {
  text: string;
  children: (describedBy: string) => ReactNode;
}) {
  const id = useId();
  const [open, setOpen] = useState(false);
  return (
    <div
      className="relative flex"
      onMouseEnter={() => setOpen(true)}
      onMouseLeave={() => setOpen(false)}
      onFocus={() => setOpen(true)}
      onBlur={() => setOpen(false)}
    >
      {children(id)}
      {open ? (
        <span
          role="tooltip"
          id={id}
          className="bg-ink text-ink-invert absolute top-[calc(100%+10px)] right-0 z-5 w-65.5 rounded-lg px-3.25 py-2.75 text-[13px] leading-relaxed shadow-[0_6px_18px_rgba(18,40,61,0.18)]"
        >
          {text}
        </span>
      ) : null}
    </div>
  );
}
