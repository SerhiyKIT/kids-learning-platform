import type { ReactNode } from "react";

/** Смуга-попередження на всю ширину контенту: іконка, текст, дія праворуч. */
export function Notice({
  title,
  children,
  action,
}: {
  title: string;
  children?: ReactNode;
  action?: ReactNode;
}) {
  return (
    <div
      role="alert"
      className="bg-notice-surface border-notice-line flex flex-wrap items-center gap-3.5 rounded-xl border px-4.5 py-4"
    >
      <span
        aria-hidden="true"
        className="border-notice-body text-notice-body flex size-5.5 flex-none items-center justify-center rounded-full border text-[13px] font-semibold"
      >
        !
      </span>
      <div className="flex min-w-50 flex-1 flex-col gap-0.5">
        <p className="text-notice-title text-[15px] font-medium">{title}</p>
        {children ? (
          <div className="text-notice-body text-sm leading-relaxed text-pretty">{children}</div>
        ) : null}
      </div>
      {action ? <div className="flex flex-none items-center gap-3">{action}</div> : null}
    </div>
  );
}

/** Кнопка всередині Notice — на білому тлі з жовтою рамкою. */
export function NoticeButton(props: React.ComponentPropsWithoutRef<"button">) {
  return (
    <button
      {...props}
      className="bg-surface border-dev-line-field text-notice-title hover:bg-notice-hover h-10 cursor-pointer rounded-lg border px-4 text-[15px] font-medium transition-colors duration-100 disabled:cursor-default disabled:opacity-60"
    />
  );
}
