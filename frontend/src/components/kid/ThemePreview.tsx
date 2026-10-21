"use client";

import type { KidTheme } from "@/lib/theme-catalog";
import { ListenButton, Mascot, StageBackdrop, StepDots } from "./KidChrome";
import { KidThemeScope } from "./KidTheme";

/**
 * A miniature of a real child-mode screen (the same stage, guide, cards and buttons) in the given
 * theme — what the moderator approves is exactly what a child will see. Decorative: not interactive.
 */
export function ThemePreview({ theme }: { theme: KidTheme }) {
  return (
    <KidThemeScope theme={theme} className="block">
      <div
        aria-hidden="true"
        inert
        className="font-kid relative flex aspect-[16/9] w-full overflow-hidden rounded-xl border border-black/10"
      >
        <StageBackdrop />
        <div className="relative z-2 flex w-full flex-col p-3">
          <div className="origin-top scale-75">
            <StepDots total={4} current={1} />
          </div>
          <div className="flex flex-1 items-center gap-3">
            <div className="flex flex-col items-center gap-2">
              <Mascot className="h-24 w-20" />
              <ListenButton className="size-12" />
            </div>
            <div className="grid flex-1 grid-cols-2 gap-3">
              {[0, 1].map((i) => (
                <span
                  key={i}
                  className={`flex h-24 items-end justify-center rounded-2xl border-4 bg-white pb-2 shadow-[0_6px_0_var(--color-kid-card-shadow)] ${
                    i === 0 ? "border-kid-teal" : "border-white"
                  }`}
                >
                  <span className="text-kid-ink text-sm font-semibold">Варіант</span>
                </span>
              ))}
            </div>
            <span className="bg-kid-teal flex size-12 flex-none items-center justify-center rounded-full border-4 border-white text-white shadow-[0_6px_0_var(--color-kid-teal-shadow)]">
              <svg className="size-1/2" viewBox="0 0 24 24" fill="none">
                <path d="M6 12h11M12.5 6.5 18 12l-5.5 5.5" stroke="currentColor" strokeWidth="2.8" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </span>
          </div>
          <p className="text-kid-ink text-center text-sm font-semibold">Текст підказки для дорослого</p>
        </div>
      </div>
    </KidThemeScope>
  );
}
