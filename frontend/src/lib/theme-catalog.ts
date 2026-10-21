// Mirror of backend ua.kidlearn.themes.ThemeCatalog — the closed lists a child picks from and the
// palette tokens a theme may set (docs/Специфікація_ассетів.md «Теми персоналізації»). Keep in sync.

export const COLOR_IDS = ["red", "orange", "yellow", "green", "blue", "purple"] as const;
export const WORLD_IDS = ["space", "sea", "forest", "dinosaurs", "flowers", "cars"] as const;
export const MOOD_IDS = ["calm", "bright"] as const;
export const GUIDE_ACCESSORIES = ["none", "cap", "scarf", "helmet", "crown_flowers", "goggles"] as const;

export type ColorId = (typeof COLOR_IDS)[number];
export type WorldId = (typeof WORLD_IDS)[number];
export type MoodId = (typeof MOOD_IDS)[number];
export type GuideAccessory = (typeof GUIDE_ACCESSORIES)[number];

/** Every theme sets exactly these --color-kid-* tokens, nothing else. */
export const PALETTE_TOKENS = [
  "canvas",
  "bg-top",
  "bg-bottom",
  "sun",
  "sun-deep",
  "teal",
  "ink",
  "ink-soft",
  "pip",
  "glow",
  "correct",
  "correct-surface",
  "scroll",
  "guide-main",
  "guide-accent",
] as const;

/**
 * The colour "spots" in the picker. These are the choice icons (assets of the picker), not theme
 * tokens: they show the child the colour itself.
 */
export const COLOR_SPOTS: Record<ColorId, string> = {
  red: "#e8574c",
  orange: "#f28c38",
  yellow: "#f5c533",
  green: "#4fb86a",
  blue: "#3d8fe0",
  purple: "#9a63d6",
};

/** Adult-facing labels (parent cabinet, admin). Children only hear the guide's lines. */
export const COLOR_LABELS: Record<ColorId, string> = {
  red: "Червоний",
  orange: "Помаранчевий",
  yellow: "Жовтий",
  green: "Зелений",
  blue: "Синій",
  purple: "Фіолетовий",
};

export const WORLD_LABELS: Record<WorldId, string> = {
  space: "Космос",
  sea: "Море",
  forest: "Ліс",
  dinosaurs: "Динозаври",
  flowers: "Квіти",
  cars: "Машинки",
};

export const MOOD_LABELS: Record<MoodId, string> = {
  calm: "Спокійно",
  bright: "Яскраво",
};

export const ACCESSORY_LABELS: Record<GuideAccessory, string> = {
  none: "Без аксесуара",
  cap: "Кепка",
  scarf: "Шарф",
  helmet: "Шолом",
  crown_flowers: "Вінок",
  goggles: "Окуляри",
};

export interface StyleAnswers {
  colorId: ColorId;
  worldId: WorldId;
  moodId: MoodId;
}

/** The approved theme child mode applies (theme_default until the child's own is approved). */
export interface KidTheme {
  id: string;
  palette: Record<string, string>;
  worldId: string | null;
  guideAccessory: string;
}

/** GET/PUT /api/children/{id}/style */
export interface StyleResponse {
  consented: boolean;
  answers: StyleAnswers | null;
  theme: KidTheme;
  /** The answers' theme isn't approved yet — the guide is "still drawing". */
  pending: boolean;
  /** A new approved theme the guide hasn't announced yet. */
  announce: boolean;
}

const HEX = /^#[0-9a-fA-F]{6}$/;

/**
 * CSS variables for a palette: only known tokens, only #RRGGBB values — nothing else gets through.
 * The three shadow variables are derived from the tokens (a darker, translucent shade), the way
 * theme_default's own shadows relate to its colours.
 */
export function paletteVariables(palette: Record<string, string> | undefined): Record<string, string> {
  const vars: Record<string, string> = {};
  for (const token of PALETTE_TOKENS) {
    const value = palette?.[token];
    if (typeof value === "string" && HEX.test(value)) vars[`--color-kid-${token}`] = value;
  }
  const shadow = (token: string, darken: number, alpha: number) => {
    const value = vars[`--color-kid-${token}`];
    return value ? shade(value, darken, alpha) : undefined;
  };
  const derived = {
    "--color-kid-sun-shadow": shadow("sun", 0.22, 0.35),
    "--color-kid-teal-shadow": shadow("teal", 0.34, 0.35),
    "--color-kid-card-shadow": shadow("bg-bottom", 0.26, 0.24),
  };
  for (const [name, value] of Object.entries(derived)) if (value) vars[name] = value;
  return vars;
}

function shade(hex: string, darken: number, alpha: number): string {
  const channels = [1, 3, 5].map((i) => Math.round(parseInt(hex.slice(i, i + 2), 16) * (1 - darken)));
  return `rgba(${channels.join(", ")}, ${alpha})`;
}

export function isWorldId(value: string | null | undefined): value is WorldId {
  return (WORLD_IDS as readonly string[]).includes(value ?? "");
}
