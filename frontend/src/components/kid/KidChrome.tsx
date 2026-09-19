"use client";

import { useRouter } from "next/navigation";

/** Тепле тло дитячого режиму з двома м'якими колами. Займає весь екран. */
export function KidStage({ children }: { children: React.ReactNode }) {
  return (
    <div className="font-kid relative flex h-screen w-screen overflow-hidden bg-[radial-gradient(120%_90%_at_12%_20%,#fff3dd_0%,#ffe0b6_55%,#ffd39c_100%)]">
      <span
        aria-hidden="true"
        className="absolute -bottom-45 -left-30 size-130 rounded-full bg-[#ffd08a] opacity-55"
      />
      <span
        aria-hidden="true"
        className="absolute -top-40 -right-35 size-115 rounded-full bg-[#ffe1a8] opacity-60"
      />
      {children}
    </div>
  );
}

/** Вихід із дитячого режиму — маленький і в куті, щоб дитина не тиснула випадково. */
export function ExitButton({ href = "/dashboard" }: { href?: string }) {
  const router = useRouter();
  return (
    <button
      type="button"
      onClick={() => router.push(href)}
      aria-label="Вихід для дорослих"
      className="text-kid-ink-soft absolute top-4.5 right-4.5 z-10 flex size-11.5 cursor-pointer items-center justify-center rounded-[14px] border-[1.5px] border-[rgba(126,88,44,0.25)] bg-white/60 hover:bg-white/90"
    >
      <svg width="20" height="20" viewBox="0 0 20 20" fill="none" aria-hidden="true">
        <path d="M12 3.5H15.5V16.5H12" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M9.5 13.5 6 10l3.5-3.5M6 10h6" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    </button>
  );
}

/**
 * Маскот-провідник. Справжня ілюстрація ще не намальована — до того часу тут
 * штрихований силует того самого розміру, щоб композиція не «стрибнула» при заміні
 * (див. docs/Специфікація_ассетів.md).
 */
export function Mascot({ size = "lg" }: { size?: "lg" | "sm" }) {
  const big = size === "lg";
  return (
    <div
      className={`relative animate-[bob_4.2s_ease-in-out_infinite] ${big ? "h-60 w-50" : "h-42 w-35"}`}
      role="img"
      aria-label="Маскот-провідник"
    >
      <span className="absolute inset-0 rounded-[46%_46%_40%_40%] border-5 border-white bg-[repeating-linear-gradient(135deg,#b9e7dd_0_12px,#a9dfd4_12px_24px)] shadow-[0_16px_0_rgba(47,179,163,0.25)]" />
    </div>
  );
}

/** Велика жовта кнопка «послухати». Головна кнопка дитячого режиму — мінімум 80px. */
export function ListenButton({
  onClick,
  size = "lg",
  label = "Послухати",
}: {
  onClick?: () => void;
  size?: "lg" | "sm";
  label?: string;
}) {
  const big = size === "lg";
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={label}
      className={`bg-kid-sun text-kid-sun-deep shadow-kid-sun-shadow flex cursor-pointer items-center justify-center rounded-full border-6 border-white shadow-[0_12px_0_var(--color-kid-sun-shadow)] animate-[pulsedot_2.6s_ease-in-out_infinite] ${
        big ? "size-27" : "size-20"
      }`}
    >
      <svg width={big ? 46 : 40} height={big ? 46 : 40} viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M4 9.5h3.2L12 5.5v13l-4.8-4H4v-5Z" fill="currentColor" />
        <path d="M15.5 9c1.2 1.1 1.2 4.9 0 6M18.4 6.6c2.2 2.2 2.2 8.6 0 10.8" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" />
      </svg>
    </button>
  );
}

/** Галочка-печатка «пройдено». */
export function DoneStamp({ className = "" }: { className?: string }) {
  return (
    <span
      aria-hidden="true"
      className={`bg-kid-teal flex size-23 -rotate-12 items-center justify-center rounded-full border-6 border-white shadow-[0_8px_0_var(--color-kid-teal-shadow)] ${className}`}
    >
      <svg width="44" height="44" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M5.5 12.8l4.2 4.2L18.5 8" stroke="#ffffff" strokeWidth="3.4" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    </span>
  );
}

/** Смужка-прогрес зі сцен: пройдені — бірюзові, поточна — жовта і більша. */
export function StepDots({ total, current }: { total: number; current: number }) {
  return (
    <div className="relative z-2 flex flex-none items-center justify-center gap-4.5 px-21 pt-5 pb-1.5">
      {Array.from({ length: total }, (_, i) => {
        const done = i < current;
        const isCurrent = i === current;
        return (
          <span
            key={i}
            className={`rounded-full border-4 border-white shadow-[0_4px_0_rgba(190,146,90,0.22)] ${
              isCurrent
                ? "bg-kid-sun size-8.5 animate-[pulsedot_2.2s_ease-in-out_infinite]"
                : done
                  ? "bg-kid-teal size-6.5"
                  : "size-6.5 bg-[#f0dcb6]"
            }`}
          />
        );
      })}
    </div>
  );
}

/**
 * Заглушка ілюстрації сцени/уроку. Той самий контракт, що й SceneAsset у scene-engine:
 * єдине місце, яке знає, що справжніх картинок ще немає.
 */
const KID_STRIPES = [
  ["#ffd7c2", "#ffc7ac"],
  ["#d8dcf7", "#c9ceef"],
  ["#ffe3a6", "#ffd782"],
  ["#cfe9e2", "#bfe1d8"],
  ["#f0dcef", "#e6cde5"],
  ["#dde7c9", "#cfdcb5"],
];

export function stripeFor(seed: string): string {
  let hash = 0;
  for (let i = 0; i < seed.length; i += 1) hash = (hash * 31 + seed.charCodeAt(i)) % 9973;
  const [a, b] = KID_STRIPES[hash % KID_STRIPES.length];
  return `repeating-linear-gradient(135deg, ${a} 0 14px, ${b} 14px 28px)`;
}
