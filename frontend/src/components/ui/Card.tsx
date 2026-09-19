import type { ComponentPropsWithoutRef, ElementType, ReactNode } from "react";

/** Картка-контейнер форми: шапка / тіло / підвал, розділені тонкими лініями. */
export function Card<T extends ElementType = "div">({
  children,
  as,
  ...rest
}: CardProps<T>) {
  const Tag = (as ?? "div") as ElementType;
  return (
    <Tag
      {...rest}
      className="bg-surface border-line shadow-card overflow-hidden rounded-xl border"
    >
      {children}
    </Tag>
  );
}

type CardProps<T extends ElementType> = {
  children: ReactNode;
  as?: T;
} & Omit<ComponentPropsWithoutRef<T>, "children" | "as">;

export function CardHeader({ title, description }: { title: string; description?: string }) {
  return (
    <div className="border-line-soft border-b px-7 pt-6.5 pb-5.5">
      <h2 className="text-[22px] font-medium tracking-[-0.01em]">{title}</h2>
      {description ? (
        <p className="text-ink-soft mt-2 text-[15px] leading-relaxed text-pretty">{description}</p>
      ) : null}
    </div>
  );
}

export function CardBody({ children }: { children: ReactNode }) {
  return <div className="flex flex-col gap-5 px-7 pt-6 pb-6.5">{children}</div>;
}

export function CardFooter({ children }: { children: ReactNode }) {
  return (
    <div className="border-line-soft flex flex-col gap-4 border-t px-7 pt-5.5 pb-6.5">
      {children}
    </div>
  );
}

/** Заголовок сторінки над карткою. */
export function PageHeading({ title, description }: { title: string; description: string }) {
  return (
    <div className="flex flex-col gap-2.5">
      <h1 className="text-[34px] leading-[1.15] font-medium tracking-[-0.02em]">{title}</h1>
      <p className="text-ink-muted max-w-[480px] text-base leading-relaxed text-pretty">
        {description}
      </p>
    </div>
  );
}

/** Вузька центрована колонка авторизаційних екранів. */
export function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <main className="flex flex-1 justify-center px-6 pt-18 pb-24">
      <div className="flex w-full max-w-[560px] flex-col gap-7">{children}</div>
    </main>
  );
}

/** Секція-картка сторінки: заголовок меншого рангу, тіло без падінгів (рядки самі їх задають). */
export function Section({ children }: { children: ReactNode }) {
  return (
    <section className="bg-surface border-line shadow-card flex flex-col overflow-hidden rounded-xl border">
      {children}
    </section>
  );
}

export function SectionHeader({
  title,
  description,
  aside,
}: {
  title: string;
  description?: string;
  aside?: ReactNode;
}) {
  return (
    <div className="border-line-soft flex items-baseline justify-between gap-4 border-b px-6 pt-5 pb-4">
      <div>
        <h2 className="text-xl font-medium tracking-[-0.01em]">{title}</h2>
        {description ? (
          <p className="text-ink-soft mt-1.5 text-sm leading-relaxed">{description}</p>
        ) : null}
      </div>
      {aside ? <span className="text-ink-icon flex-none text-sm">{aside}</span> : null}
    </div>
  );
}
