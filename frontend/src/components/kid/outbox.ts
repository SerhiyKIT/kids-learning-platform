"use client";

import { useEffect, useState } from "react";
import { apiFetch, ApiError } from "@/lib/api";

/** Thrown to the caller when a write couldn't be delivered now and waits in the outbox. */
export class QueuedOffline extends Error {
  constructor() {
    super("Queued until the network is back");
    this.name = "QueuedOffline";
  }
}

const RETRY_MS = [2_000, 5_000, 10_000, 30_000];

/**
 * The child's results (answers, lesson completion), delivered in order. A write that fails for
 * lack of network — fetch itself throws, as opposed to an ApiError from the server — stays queued
 * and is retried when the browser is back online or on a timer, so a Wi-Fi drop mid-lesson doesn't
 * lose them (docs/UX_гайд_дитячого_режиму.md §8: the lesson goes on). A write the server refused
 * (401, 404, 409…) is dropped: retrying wouldn't change the answer. Kept in memory only, for as
 * long as child mode is open; closing the tab while offline still loses what is queued.
 */
class Outbox {
  private readonly queue: { path: string; body: string }[] = [];
  private flushing = false;
  private failures = 0;
  private timer: ReturnType<typeof setTimeout> | null = null;

  /**
   * Resolves with the response when delivered right away; otherwise rejects — with QueuedOffline
   * when the write now waits in the queue (also behind earlier waiting writes, to keep the order),
   * so the caller never waits for the network to come back.
   */
  async send<T>(path: string, body: unknown): Promise<T> {
    const pending = { path, body: JSON.stringify(body) };
    if (this.queue.length > 0) {
      this.queue.push(pending);
      void this.flush();
      throw new QueuedOffline();
    }
    try {
      return await apiFetch<T>(path, { method: "POST", body: pending.body });
    } catch (err) {
      if (err instanceof ApiError) throw err;
      this.queue.push(pending);
      this.retryLater();
      throw new QueuedOffline();
    }
  }

  readonly flush = async (): Promise<void> => {
    if (this.flushing) return;
    this.flushing = true;
    this.clearTimer();
    try {
      while (this.queue.length > 0) {
        try {
          await apiFetch(this.queue[0].path, { method: "POST", body: this.queue[0].body });
        } catch (err) {
          if (!(err instanceof ApiError)) {
            this.retryLater();
            return;
          }
        }
        this.queue.shift();
        this.failures = 0;
      }
    } finally {
      this.flushing = false;
    }
  };

  dispose(): void {
    this.clearTimer();
  }

  private retryLater(): void {
    this.clearTimer();
    const delay = RETRY_MS[Math.min(this.failures, RETRY_MS.length - 1)];
    this.failures += 1;
    this.timer = setTimeout(() => void this.flush(), delay);
  }

  private clearTimer(): void {
    if (this.timer) clearTimeout(this.timer);
    this.timer = null;
  }
}

/** One outbox per child-mode visit; retries as soon as the browser reports it is back online. */
export function useOutbox(): Outbox {
  const [outbox] = useState(() => new Outbox());
  useEffect(() => {
    window.addEventListener("online", outbox.flush);
    return () => {
      window.removeEventListener("online", outbox.flush);
      outbox.dispose();
    };
  }, [outbox]);
  return outbox;
}
