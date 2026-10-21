"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { apiFetchBlob } from "@/lib/api";

/**
 * One shared <audio> element for a whole lesson. Browsers only let audio start after a user
 * gesture; iOS Safari unlocks per element, so every line plays through this same element after the
 * «Почнімо!» tap has unlocked it (see {@link KidAudioControls.unlock}).
 *
 * Audio is fetched as a Blob and played from a blob: URL rather than pointing the element at
 * /api/audio/… directly: media elements issue Range requests, which the service-worker cache
 * (public/sw.js) can't answer from a stored 200 on every browser, and a Blob always plays.
 */
export interface KidAudioControls {
  /** Plays a line; resolves false when the browser refuses (autoplay policy, muted) or it fails. */
  play: (url: string, options?: { restart?: boolean }) => Promise<boolean>;
  /** Must run synchronously inside a tap. Unlocks audio, starting `firstUrl` right away if it's loaded. */
  unlock: (firstUrl?: string) => void;
  /** Replays the most recently requested line (tap on the mascot / «послухати»). */
  replay: () => void;
  /** Starts downloading these lines in the background so the next scene starts without a gap. */
  preload: (urls: string[]) => void;
  /**
   * Plays a line from the start and resolves when it has been heard to the end: true when it
   * ended, false if it couldn't start or was interrupted by another line. For read-aloud sequences.
   */
  playToEnd: (url: string) => Promise<boolean>;
  /**
   * While suspended (e.g. the adult's password screen is open) the current line stops and nothing
   * plays — idle hints included. Lifting it doesn't resume anything; the child taps «послухати».
   */
  suspend: (suspended: boolean) => void;
}

/** What's audible right now, for animations and the "tap to listen" nudge. */
export interface KidAudioState {
  /** The audioUrl currently being heard, or null. */
  speakingUrl: string | null;
  /** The browser refused the last line (autoplay policy / muted): a tap on «послухати» is needed. */
  blocked: boolean;
}

const NO_AUDIO: KidAudioControls = {
  play: () => Promise.resolve(false),
  unlock: () => {},
  replay: () => {},
  preload: () => {},
  playToEnd: () => Promise.resolve(false),
  suspend: () => {},
};

const ControlsContext = createContext<KidAudioControls>(NO_AUDIO);
const StateContext = createContext<KidAudioState>({ speakingUrl: null, blocked: false });

/** Stable functions only — safe as effect dependencies (they never change identity). */
export function useKidAudio(): KidAudioControls {
  return useContext(ControlsContext);
}

/** The audioUrl currently being heard, or null. Separate context so speaking changes don't re-run effects. */
export function useSpeakingUrl(): string | null {
  return useContext(StateContext).speakingUrl;
}

/** True while the browser is refusing to play (until a line plays again after a tap). */
export function useAudioBlocked(): boolean {
  return useContext(StateContext).blocked;
}

const AUDIO_PATH = /^\/api\/audio\/[0-9a-f]{64}$/;

export function KidAudioProvider({ children }: { children: ReactNode }) {
  const elementRef = useRef<HTMLAudioElement | null>(null);
  const currentUrl = useRef<string | null>(null);
  const lastRequested = useRef<string | null>(null);
  const loads = useRef(new Map<string, Promise<string>>());
  const loaded = useRef(new Map<string, string>());
  // The gesture-time unlock play(); switching src before it settles would abort it (AbortError),
  // and iOS only counts the element as unlocked once that in-gesture play() has gone through.
  const unlockPending = useRef<Promise<void> | null>(null);
  const suspended = useRef(false);
  const [speakingUrl, setSpeakingUrl] = useState<string | null>(null);
  const [blocked, setBlocked] = useState(false);

  const element = useCallback((): HTMLAudioElement => {
    if (!elementRef.current) {
      const audio = new Audio();
      audio.preload = "auto";
      audio.addEventListener("playing", () => {
        setSpeakingUrl(currentUrl.current);
        setBlocked(false);
      });
      for (const stop of ["pause", "ended", "error", "emptied"]) {
        audio.addEventListener(stop, () => setSpeakingUrl(null));
      }
      elementRef.current = audio;
    }
    return elementRef.current;
  }, []);

  const load = useCallback((url: string): Promise<string> => {
    if (!AUDIO_PATH.test(url)) return Promise.reject(new Error("not a voice-line audio URL"));
    let pending = loads.current.get(url);
    if (!pending) {
      pending = apiFetchBlob(url.slice("/api".length)).then((blob) => {
        const objectUrl = URL.createObjectURL(blob);
        loaded.current.set(url, objectUrl);
        return objectUrl;
      });
      // A failed download may be retried later (e.g. after reconnecting).
      pending.catch(() => loads.current.delete(url));
      loads.current.set(url, pending);
    }
    return pending;
  }, []);

  const play = useCallback(
    async (url: string, options?: { restart?: boolean }): Promise<boolean> => {
      if (suspended.current) return false;
      lastRequested.current = url;
      const audio = element();
      if (currentUrl.current === url && !audio.paused && !options?.restart) {
        return true; // Already playing this line (e.g. started by the «Почнімо!» tap).
      }
      try {
        const src = await load(url);
        if (unlockPending.current) await unlockPending.current;
        // A newer line took over meanwhile, or the adult's screen opened.
        if (lastRequested.current !== url || suspended.current) return false;
        if (currentUrl.current !== url || audio.src !== src) {
          audio.src = src;
          currentUrl.current = url;
        }
        audio.currentTime = 0;
        await audio.play();
        return true;
      } catch (err) {
        if (err instanceof DOMException && err.name === "NotAllowedError" && lastRequested.current === url) {
          setBlocked(true);
        }
        return false;
      }
    },
    [element, load],
  );

  const unlock = useCallback(
    (firstUrl?: string) => {
      const audio = element();
      const ready = firstUrl ? loaded.current.get(firstUrl) : undefined;
      if (firstUrl && ready) {
        lastRequested.current = firstUrl;
        currentUrl.current = firstUrl;
        audio.src = ready;
      } else {
        // Nothing downloaded yet: a silent clip still counts as "played in a gesture".
        currentUrl.current = null;
        audio.src = silentWav();
      }
      const started = audio.play().catch(() => {});
      unlockPending.current = started;
      started.finally(() => {
        if (unlockPending.current === started) unlockPending.current = null;
      });
    },
    [element],
  );

  const replay = useCallback(() => {
    if (lastRequested.current) void play(lastRequested.current, { restart: true });
  }, [play]);

  const playToEnd = useCallback(
    async (url: string): Promise<boolean> => {
      if (!(await play(url, { restart: true }))) return false;
      const audio = elementRef.current;
      if (!audio) return false;
      return new Promise<boolean>((resolve) => {
        const finish = (heard: boolean) => {
          audio.removeEventListener("ended", onEnded);
          audio.removeEventListener("pause", onStopped);
          audio.removeEventListener("emptied", onStopped);
          audio.removeEventListener("error", onStopped);
          resolve(heard);
        };
        const onEnded = () => finish(true);
        // A natural end fires "pause" just before "ended" — only a pause short of the end interrupts.
        const onStopped = () => {
          if (!audio.ended) finish(false);
        };
        audio.addEventListener("ended", onEnded);
        audio.addEventListener("pause", onStopped);
        audio.addEventListener("emptied", onStopped);
        audio.addEventListener("error", onStopped);
      });
    },
    [play],
  );

  const suspend = useCallback((on: boolean) => {
    suspended.current = on;
    if (on) elementRef.current?.pause();
  }, []);

  const preload = useCallback(
    (urls: string[]) => {
      for (const url of urls) load(url).catch(() => {});
    },
    [load],
  );

  useEffect(() => {
    const objectUrls = loaded.current;
    const pending = loads.current;
    return () => {
      elementRef.current?.pause();
      elementRef.current = null;
      objectUrls.forEach((objectUrl) => URL.revokeObjectURL(objectUrl));
      objectUrls.clear();
      pending.clear(); // never hand out a revoked blob: URL from a stale download
    };
  }, []);

  const controls = useMemo(
    () => ({ play, unlock, replay, preload, playToEnd, suspend }),
    [play, unlock, replay, preload, playToEnd, suspend],
  );
  const state = useMemo(() => ({ speakingUrl, blocked }), [speakingUrl, blocked]);

  return (
    <ControlsContext.Provider value={controls}>
      <StateContext.Provider value={state}>{children}</StateContext.Provider>
    </ControlsContext.Provider>
  );
}

let silentWavUrl: string | null = null;

/** 0.1 s of 8-bit mono silence as a data: URL — just enough for the unlock gesture to "play" something. */
function silentWav(): string {
  if (silentWavUrl) return silentWavUrl;
  const samples = 800;
  const bytes = new Uint8Array(44 + samples);
  const view = new DataView(bytes.buffer);
  const ascii = (offset: number, text: string) => [...text].forEach((c, i) => view.setUint8(offset + i, c.charCodeAt(0)));
  ascii(0, "RIFF");
  view.setUint32(4, 36 + samples, true);
  ascii(8, "WAVE");
  ascii(12, "fmt ");
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true); // PCM
  view.setUint16(22, 1, true); // mono
  view.setUint32(24, 8000, true); // sample rate
  view.setUint32(28, 8000, true); // byte rate
  view.setUint16(32, 1, true); // block align
  view.setUint16(34, 8, true); // bits per sample
  ascii(36, "data");
  view.setUint32(40, samples, true);
  bytes.fill(128, 44); // 8-bit PCM silence is the midpoint
  silentWavUrl = `data:audio/wav;base64,${btoa(String.fromCharCode(...bytes))}`;
  return silentWavUrl;
}
