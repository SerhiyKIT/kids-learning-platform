"use client";

import Link from "next/link";
import { stripeFor } from "./KidChrome";

/**
 * Sticker art. Real sticker illustrations aren't drawn yet (docs/Специфікація_ассетів.md): until
 * then a sticker is the lesson's placeholder stripes with its topic's pictogram, a locked one is a
 * grey silhouette of the same shape, and a topic sticker is the bigger pictogram on its own.
 */
export function StickerArt({
  seed,
  topic,
  locked = false,
  className = "",
}: {
  seed: string;
  topic: string;
  locked?: boolean;
  className?: string;
}) {
  return (
    <span
      aria-hidden="true"
      className={`relative flex items-center justify-center rounded-full border-[clamp(4px,1.2dvh,7px)] ${
        locked
          ? "border-kid-pip bg-kid-pip"
          : "border-white shadow-[0_8px_0_var(--color-kid-card-shadow)]"
      } ${className}`}
      style={locked ? undefined : { background: stripeFor(seed) }}
    >
      <TopicIcon topic={topic} className={`size-[56%] ${locked ? "opacity-45 grayscale" : ""}`} />
    </span>
  );
}

/** Pictogram of a lesson topic (docs/lesson-scenario.schema.json "topic"). */
export function TopicIcon({ topic, className = "" }: { topic: string; className?: string }) {
  if (topic === "road_safety") {
    return (
      <svg viewBox="0 0 48 48" className={className} aria-hidden="true">
        <rect x="14" y="3" width="20" height="42" rx="7" fill="#4a4f5c" />
        <circle cx="24" cy="13" r="5.2" fill="#ff6b5b" />
        <circle cx="24" cy="24" r="5.2" fill="#ffc13b" />
        <circle cx="24" cy="35" r="5.2" fill="#52c97a" />
      </svg>
    );
  }
  if (topic === "emergency_numbers") {
    return (
      <svg viewBox="0 0 48 48" className={className} aria-hidden="true">
        <path
          d="M14 6c2 0 3 1 4 3l3 7c.6 1.6.2 3-1 4l-3 2.4c2 4.4 5.2 7.6 9.6 9.6L29 29c1-1.2 2.4-1.6 4-1l7 3c2 1 3 2 3 4v4c0 3-2.4 5-5.4 4.6C21 41 7 27 4.4 9.4 4 6.4 6 4 9 4h5Z"
          fill="#ff6b5b"
        />
      </svg>
    );
  }
  return (
    <svg viewBox="0 0 48 48" className={className} aria-hidden="true">
      <path d="m24 4 6 13 14 1.6-10.5 9.6L36.6 42 24 34.8 11.4 42l3.1-13.8L4 18.6 18 17Z" fill="#ffc13b" />
    </svg>
  );
}

/** «Мої наліпки»: an icon, ≥ 64px. */
export function AlbumLink({ href, className = "" }: { href: string; className?: string }) {
  return (
    <Link
      href={href}
      aria-label="Мої наліпки"
      className={`bg-kid-teal flex size-[clamp(68px,16dvh,92px)] flex-none items-center justify-center rounded-[28%] border-[clamp(4px,1dvh,6px)] border-white text-white no-underline shadow-[0_10px_0_var(--color-kid-teal-shadow)] ${className}`}
    >
      <AlbumIcon className="size-[58%]" />
    </Link>
  );
}

export function AlbumIcon({ className = "" }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" className={className} aria-hidden="true">
      <rect x="3.5" y="3.5" width="17" height="17" rx="3.5" stroke="currentColor" strokeWidth="2.2" />
      <circle cx="9" cy="9.5" r="2.6" fill="currentColor" />
      <path d="m12.8 16.6 1.2-3.4 1.2 3.4-3-2.1h3.6Z" fill="currentColor" stroke="currentColor" strokeWidth="1.2" strokeLinejoin="round" />
      <circle cx="16" cy="9" r="1.6" fill="currentColor" />
      <circle cx="8.4" cy="15.6" r="1.6" fill="currentColor" />
    </svg>
  );
}

/** «Обрати оформлення»: a brush icon, ≥ 64px. Only shown with the parent's style consent. */
export function StyleLink({ href }: { href: string }) {
  return (
    <Link
      href={href}
      aria-label="Обрати оформлення"
      className="bg-kid-sun text-kid-sun-deep flex size-[clamp(68px,16dvh,92px)] flex-none items-center justify-center rounded-[28%] border-[clamp(4px,1dvh,6px)] border-white no-underline shadow-[0_10px_0_var(--color-kid-sun-shadow)]"
    >
      <svg viewBox="0 0 24 24" fill="none" className="size-[58%]" aria-hidden="true">
        <path d="M14.5 4.5 19.5 9.5 11 18l-5-5z" fill="currentColor" />
        <path d="M6 13l5 5c-1 2.5-3.5 3.5-6.5 2.5C3.5 17.5 4 15 6 13Z" fill="currentColor" opacity="0.7" />
        <path d="M16 3l5 5" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" />
      </svg>
    </Link>
  );
}
