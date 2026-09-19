import Link from "next/link";
import type { ReactNode } from "react";

const base =
  "bg-surface border-brand-line text-brand hover:bg-brand-tint inline-flex h-9.5 cursor-pointer items-center rounded-lg border px-3.5 text-sm font-medium transition-colors duration-100";

/** Другорядна дія в бренд-кольорі — як кнопка або як посилання. */
export function SecondaryButton(props: React.ComponentPropsWithoutRef<"button">) {
  return <button {...props} className={`${base} ${props.className ?? ""}`} />;
}

export function SecondaryLink({ href, children }: { href: string; children: ReactNode }) {
  return (
    <Link href={href} className={`${base} no-underline`}>
      {children}
    </Link>
  );
}

/** Нейтральна дія: сіра рамка, темний текст. */
export function QuietButton(props: React.ComponentPropsWithoutRef<"button">) {
  return (
    <button
      {...props}
      className={`bg-surface border-line-field text-avatar-ink hover:bg-canvas inline-flex h-9.5 flex-none cursor-pointer items-center rounded-lg border px-3.5 text-sm font-medium transition-colors duration-100 disabled:opacity-60 ${props.className ?? ""}`}
    />
  );
}

/** Руйнівна дія. */
export function DangerButton(props: React.ComponentPropsWithoutRef<"button">) {
  return (
    <button
      {...props}
      className={`bg-danger hover:bg-danger-hover inline-flex h-11 cursor-pointer items-center rounded-lg px-5 text-[15px] font-medium text-white transition-colors duration-100 disabled:opacity-50 ${props.className ?? ""}`}
    />
  );
}
