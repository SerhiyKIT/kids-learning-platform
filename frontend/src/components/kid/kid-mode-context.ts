"use client";

import { createContext, useContext } from "react";
import type { ShellLine } from "@/lib/api-types";
import type { StyleResponse } from "@/lib/theme-catalog";

/** Keys of the guide's fixed child-mode lines (backend: ua.kidlearn.aipipeline.KidShellLines). */
export type ShellLineKey =
  | "greet_1"
  | "greet_2"
  | "greet_3"
  | "turn_device"
  | "celebrate"
  | "sticker_new"
  | "replay_joy"
  | "album_intro"
  | "sleep_tired"
  | "sleep_again"
  | "style_color"
  | "style_world"
  | "style_mood"
  | "style_drawing"
  | "theme_ready"
  | `color_${string}`
  | `world_${string}`
  | `mood_${string}`;

export interface KidMode {
  childId: string;
  /** A guide line, once loaded; its audioUrl is missing until an admin has voiced the lines. */
  line: (key: ShellLineKey) => ShellLine | undefined;
  /** The exit button was held long enough: open the adult's password screen. */
  requestExit: () => void;
  /** The child's style (consent, answers, the approved theme in use); null until loaded. */
  style: StyleResponse | null;
  /** The style request has finished (loaded or failed), so screens can stop waiting for it. */
  styleReady: boolean;
  /** Replaces the style (after the child chose, or the guide announced the theme) — re-themes at once. */
  setStyle: (style: StyleResponse) => void;
}

export const KidModeContext = createContext<KidMode>({
  childId: "",
  line: () => undefined,
  requestExit: () => {},
  style: null,
  styleReady: false,
  setStyle: () => {},
});

export function useKidMode(): KidMode {
  return useContext(KidModeContext);
}
