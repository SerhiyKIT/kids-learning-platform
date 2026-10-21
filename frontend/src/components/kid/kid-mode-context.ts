"use client";

import { createContext, useContext } from "react";
import type { ShellLine } from "@/lib/api-types";

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
  | "sleep_again";

export interface KidMode {
  childId: string;
  /** A guide line, once loaded; its audioUrl is missing until an admin has voiced the lines. */
  line: (key: ShellLineKey) => ShellLine | undefined;
  /** The exit button was held long enough: open the adult's password screen. */
  requestExit: () => void;
}

export const KidModeContext = createContext<KidMode>({
  childId: "",
  line: () => undefined,
  requestExit: () => {},
});

export function useKidMode(): KidMode {
  return useContext(KidModeContext);
}
