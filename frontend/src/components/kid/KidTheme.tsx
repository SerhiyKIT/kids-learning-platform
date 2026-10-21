"use client";

import { createContext, useContext, useId, type CSSProperties, type ReactNode } from "react";
import { isWorldId, paletteVariables, type KidTheme, type WorldId } from "@/lib/theme-catalog";

const KidThemeContext = createContext<Pick<KidTheme, "worldId" | "guideAccessory">>({
  worldId: null,
  guideAccessory: "none",
});

/** The current theme's world decor and guide accessory (palette goes through CSS variables). */
export function useKidTheme() {
  return useContext(KidThemeContext);
}

/**
 * Applies an approved theme to everything inside: its palette as the --color-kid-* variables (the
 * only thing a theme can change — see paletteVariables), plus the world decor and guide accessory
 * for KidStage and Mascot. Without a theme the theme_default values from globals.css apply.
 * `contents` by default, so it adds no box of its own.
 */
export function KidThemeScope({
  theme,
  className = "contents",
  children,
}: {
  theme: KidTheme | null;
  className?: string;
  children: ReactNode;
}) {
  return (
    <div className={`kid-theme ${className}`} style={paletteVariables(theme?.palette) as CSSProperties} data-theme-id={theme?.id}>
      <KidThemeContext.Provider value={{ worldId: theme?.worldId ?? null, guideAccessory: theme?.guideAccessory ?? "none" }}>
        {children}
      </KidThemeContext.Provider>
    </div>
  );
}

/**
 * A world's decor pack — for now a placeholder pattern drawn only with theme tokens (the real
 * illustrations come in M4; docs/Специфікація_ассетів.md «Теми персоналізації»).
 */
export function WorldDecor({ worldId, className = "" }: { worldId: string | null; className?: string }) {
  const id = useId();
  if (!isWorldId(worldId)) return null;
  return (
    <svg aria-hidden="true" data-world={worldId} className={`pointer-events-none absolute inset-0 size-full ${className}`}>
      <defs>
        <pattern id={id} width="140" height="140" patternUnits="userSpaceOnUse">
          <DecorTile worldId={worldId} />
        </pattern>
      </defs>
      <rect width="100%" height="100%" fill={`url(#${id})`} />
    </svg>
  );
}

const ink = { fill: "var(--color-kid-ink-soft)" };
const glow = { fill: "var(--color-kid-glow)" };
const teal = { fill: "var(--color-kid-teal)" };
const accent = { fill: "var(--color-kid-guide-accent)" };

function DecorTile({ worldId }: { worldId: WorldId }) {
  switch (worldId) {
    case "space":
      return (
        <g opacity="0.32">
          <path d="M30 14l4 10 10 4-10 4-4 10-4-10-10-4 10-4z" style={glow} />
          <path d="M100 74l3 7 7 3-7 3-3 7-3-7-7-3 7-3z" style={ink} />
          <circle cx="104" cy="22" r="3" style={ink} />
          <circle cx="40" cy="104" r="2.5" style={ink} />
          <circle cx="70" cy="50" r="9" style={accent} />
        </g>
      );
    case "sea":
      return (
        <g opacity="0.3" fill="none" strokeWidth="6" strokeLinecap="round" style={{ stroke: "var(--color-kid-teal)" }}>
          <path d="M6 40c12-12 24-12 36 0s24 12 36 0 24-12 36 0" />
          <path d="M-20 110c12-12 24-12 36 0s24 12 36 0 24-12 36 0 24-12 36 0" />
        </g>
      );
    case "forest":
      return (
        <g opacity="0.3">
          <path d="M34 20l20 36H14z" style={accent} />
          <rect x="30" y="56" width="8" height="12" style={ink} />
          <path d="M100 80l18 32H82z" style={teal} />
          <rect x="96" y="112" width="8" height="10" style={ink} />
        </g>
      );
    case "dinosaurs":
      return (
        <g opacity="0.3">
          <path d="M10 60l12-18 12 18 12-18 12 18 12-18 12 18z" style={accent} />
          <ellipse cx="104" cy="104" rx="9" ry="12" style={ink} />
          <circle cx="94" cy="88" r="3.5" style={ink} />
          <circle cx="104" cy="85" r="3.5" style={ink} />
          <circle cx="114" cy="88" r="3.5" style={ink} />
        </g>
      );
    case "flowers":
      return (
        <g opacity="0.35">
          {[0, 72, 144, 216, 288].map((a) => (
            <circle key={a} cx={36 + 11 * Math.cos((a * Math.PI) / 180)} cy={36 + 11 * Math.sin((a * Math.PI) / 180)} r="8" style={glow} />
          ))}
          <circle cx="36" cy="36" r="6" style={{ fill: "var(--color-kid-sun)" }} />
          {[0, 72, 144, 216, 288].map((a) => (
            <circle key={`b${a}`} cx={104 + 8 * Math.cos((a * Math.PI) / 180)} cy={100 + 8 * Math.sin((a * Math.PI) / 180)} r="6" style={accent} />
          ))}
        </g>
      );
    case "cars":
      return (
        <g opacity="0.3">
          <rect x="14" y="30" width="46" height="18" rx="8" style={teal} />
          <path d="M24 30l8-10h16l8 10z" style={teal} />
          <circle cx="26" cy="50" r="6" style={ink} />
          <circle cx="50" cy="50" r="6" style={ink} />
          <rect x="80" y="96" width="40" height="16" rx="7" style={accent} />
          <circle cx="90" cy="114" r="5" style={ink} />
          <circle cx="110" cy="114" r="5" style={ink} />
        </g>
      );
  }
}

/** The guide's accessory — a placeholder mark on the mascot until the real character art (M4). */
export function AccessoryMark({ accessory }: { accessory: string }) {
  if (accessory === "none") return null;
  return (
    <svg viewBox="0 0 100 120" aria-hidden="true" data-accessory={accessory} className="pointer-events-none absolute inset-0 size-full overflow-visible">
      {accessory === "cap" ? (
        <g style={{ fill: "var(--color-kid-sun)" }}>
          <path d="M22 26a28 22 0 0 1 56 0z" />
          <rect x="44" y="22" width="44" height="8" rx="4" />
        </g>
      ) : null}
      {accessory === "scarf" ? (
        <g style={{ fill: "var(--color-kid-teal)" }}>
          <rect x="12" y="78" width="76" height="12" rx="6" />
          <rect x="62" y="84" width="12" height="26" rx="5" />
        </g>
      ) : null}
      {accessory === "helmet" ? (
        <path d="M14 46a36 38 0 0 1 72 0" fill="none" strokeWidth="9" strokeLinecap="round" style={{ stroke: "var(--color-kid-ink-soft)" }} />
      ) : null}
      {accessory === "crown_flowers" ? (
        <g>
          {[24, 40, 56, 72].map((x, i) => (
            <circle key={x} cx={x + 2} cy={i % 2 ? 12 : 16} r="8" style={{ fill: i % 2 ? "var(--color-kid-sun)" : "var(--color-kid-glow)" }} />
          ))}
        </g>
      ) : null}
      {accessory === "goggles" ? (
        <g fill="none" strokeWidth="6" style={{ stroke: "var(--color-kid-ink)" }}>
          <path d="M8 40h84" />
          <circle cx="34" cy="40" r="12" style={{ fill: "var(--color-kid-bg-top)" }} />
          <circle cx="66" cy="40" r="12" style={{ fill: "var(--color-kid-bg-top)" }} />
        </g>
      ) : null}
    </svg>
  );
}
