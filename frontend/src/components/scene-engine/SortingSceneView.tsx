"use client";

import { useEffect, useRef, useState, type PointerEvent as ReactPointerEvent } from "react";
import type { SortingCategory, SortingScene, VoiceLine } from "@/lib/scenario-types";
import { stripeFor } from "@/components/kid/KidChrome";
import { useKidAudio } from "./KidAudio";
import type { SceneProps } from "./SceneRenderer";
import { NextButton, SceneShell } from "./SceneShell";
import { useSceneAssistant } from "./useSceneAssistant";
import { VoiceLinePlayer } from "./VoiceLinePlayer";

type CategoryId = SortingCategory["id"];

/** Movement below this is a tap, not a drag (no long-press or double-tap gestures exist). */
const TAP_SLOP_PX = 8;
/** "Magnetic" capture: the basket's hit zone grows by this much on every side… */
const MAGNET_ZONE_PX = 56;
/** …and inside it the card is pulled this far toward the basket's centre. */
const MAGNET_PULL = 0.35;
/** How long a released card glides back (matches duration-300 below). */
const RETURN_MS = 300;
/**
 * After the last card lands, the tray stays a moment before «Далі» replaces it: the card visibly
 * settles, and — crucially — the touched card isn't removed from the DOM in the middle of its own
 * touch sequence (pointerup → touchend), which leaves Chrome's tap recognizer swallowing the NEXT
 * tap (the child's tap on «Далі» would do nothing).
 */
const FINISH_DELAY_MS = 350;

interface Drag {
  index: number;
  pointerId: number;
  startX: number;
  startY: number;
  originX: number;
  originY: number;
  dx: number;
  dy: number;
  moved: boolean;
}

/**
 * Sorting: put every picture in the right one of two baskets (docs/UX_гайд_дитячого_режиму.md §2:
 * only tap and simple, *magnetic* drag). Drag works with mouse, touch and stylus (Pointer Events,
 * `touch-action: none` on the cards): inside a basket's enlarged zone the card is pulled toward it
 * and dropped there on release; released anywhere else it glides back. Tap a card, then tap a
 * basket, does the same for children (and adults) who can't drag. Tapping a basket with no card
 * picked says the basket's name.
 *
 * A right basket: the card settles in and feedback_correct is heard. A wrong one: feedback_wrong,
 * the card goes back, and the hint level rises (not on control scenes). When every card is placed,
 * «Далі» appears.
 *
 * Answers: one event per placement attempt — chosenOption = "<itemIndex>:<categoryId>" (items have
 * no id in the schema, so their index identifies them), tryNo = that item's attempt number. The
 * scene counts as right on the first try only if EVERY item was placed right on its first attempt.
 */
export function SortingSceneView({ scene, progress, onAnswer, onScored, onAdvance }: SceneProps<SortingScene>) {
  const { play } = useKidAudio();
  const [placed, setPlaced] = useState<(CategoryId | null)[]>(() => scene.items.map(() => null));
  const [selected, setSelected] = useState<number | null>(null);
  const [drag, setDrag] = useState<Drag | null>(null);
  const [hover, setHover] = useState<CategoryId | null>(null);
  // The card gliding back after a miss: no "breathing" meanwhile, since that animation also drives
  // transform and would make the card jump instead of glide.
  const [returning, setReturning] = useState<number | null>(null);
  const returnTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [finished, setFinished] = useState(false);
  const finishTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [caption, setCaption] = useState<{ line: VoiceLine; n: number }>({ line: scene.setup, n: 0 });
  const [captionReady, setCaptionReady] = useState(false);
  const attempts = useRef<number[]>(scene.items.map(() => 0));
  const missedFirstTry = useRef(false);
  const basketRefs = useRef<Partial<Record<CategoryId, HTMLButtonElement | null>>>({});

  useEffect(
    () => () => {
      if (returnTimer.current) clearTimeout(returnTimer.current);
      if (finishTimer.current) clearTimeout(finishTimer.current);
    },
    [],
  );

  const done = placed.every((p) => p !== null);
  const { hint, revealCorrect, touch, registerMistake, guideTap, hintLevel } = useSceneAssistant({
    assistant: scene.assistant,
    isControl: scene.is_control,
    waiting: !done && drag === null,
  });
  // After hint 2: show the next card still to place, and where it goes.
  const revealIndex = revealCorrect ? placed.findIndex((p) => p === null) : -1;
  const revealTarget = revealIndex >= 0 ? scene.items[revealIndex].target : null;

  function say(line: VoiceLine) {
    setCaptionReady(false);
    setCaption((prev) => ({ line, n: prev.n + 1 }));
  }

  function place(index: number, category: CategoryId) {
    const item = scene.items[index];
    const correct = item.target === category;
    attempts.current[index] += 1;
    onAnswer({
      sceneKey: scene.key,
      tryNo: attempts.current[index],
      chosenOption: `${index}:${category}`,
      isCorrect: correct,
      hintsUsed: hintLevel,
    });
    setSelected(null);
    if (correct) {
      const next = placed.map((p, i) => (i === index ? category : p));
      setPlaced(next);
      say(item.feedback_correct);
      touch();
      if (next.every((p) => p !== null)) {
        onScored(!missedFirstTry.current);
        finishTimer.current = setTimeout(() => setFinished(true), FINISH_DELAY_MS);
      }
    } else {
      missedFirstTry.current = true;
      say(item.feedback_wrong);
      registerMistake();
    }
  }

  function basketAt(x: number, y: number): CategoryId | null {
    let best: { id: CategoryId; distance: number } | null = null;
    for (const category of scene.categories) {
      const rect = basketRefs.current[category.id]?.getBoundingClientRect();
      if (!rect) continue;
      const inside =
        x >= rect.left - MAGNET_ZONE_PX &&
        x <= rect.right + MAGNET_ZONE_PX &&
        y >= rect.top - MAGNET_ZONE_PX &&
        y <= rect.bottom + MAGNET_ZONE_PX;
      if (!inside) continue;
      const distance = Math.hypot(x - (rect.left + rect.width / 2), y - (rect.top + rect.height / 2));
      if (!best || distance < best.distance) best = { id: category.id, distance };
    }
    return best?.id ?? null;
  }

  function onPointerDown(event: ReactPointerEvent<HTMLButtonElement>, index: number) {
    if (placed[index] !== null) return;
    touch();
    event.currentTarget.setPointerCapture(event.pointerId);
    const rect = event.currentTarget.getBoundingClientRect();
    setDrag({
      index,
      pointerId: event.pointerId,
      startX: event.clientX,
      startY: event.clientY,
      originX: rect.left + rect.width / 2,
      originY: rect.top + rect.height / 2,
      dx: 0,
      dy: 0,
      moved: false,
    });
  }

  function onPointerMove(event: ReactPointerEvent<HTMLButtonElement>) {
    if (!drag || event.pointerId !== drag.pointerId) return;
    const rawX = event.clientX - drag.startX;
    const rawY = event.clientY - drag.startY;
    const moved = drag.moved || Math.hypot(rawX, rawY) > TAP_SLOP_PX;
    const target = moved ? basketAt(event.clientX, event.clientY) : null;
    let dx = rawX;
    let dy = rawY;
    const rect = target ? basketRefs.current[target]?.getBoundingClientRect() : undefined;
    if (rect) {
      const cardX = drag.originX + rawX;
      const cardY = drag.originY + rawY;
      dx += (rect.left + rect.width / 2 - cardX) * MAGNET_PULL;
      dy += (rect.top + rect.height / 2 - cardY) * MAGNET_PULL;
    }
    setHover(target);
    setDrag({ ...drag, dx, dy, moved });
  }

  function onPointerUp(event: ReactPointerEvent<HTMLButtonElement>) {
    if (!drag || event.pointerId !== drag.pointerId) return;
    const { index, moved } = drag;
    const target = moved ? basketAt(event.clientX, event.clientY) : null;
    setDrag(null);
    setHover(null);
    if (moved) glideBack(index);
    if (!moved) {
      setSelected((current) => (current === index ? null : index));
    } else if (target) {
      place(index, target);
    }
    // Released elsewhere: dropping the transform lets the card glide back to its slot.
  }

  function onPointerCancel() {
    if (drag) glideBack(drag.index);
    setDrag(null);
    setHover(null);
  }

  function glideBack(index: number) {
    setReturning(index);
    if (returnTimer.current) clearTimeout(returnTimer.current);
    returnTimer.current = setTimeout(() => setReturning(null), RETURN_MS);
  }

  function onBasketTap(category: SortingCategory) {
    touch();
    if (selected !== null) {
      place(selected, category.id);
    } else if (category.label.audioUrl) {
      void play(category.label.audioUrl, { restart: true });
    }
  }

  const baskets = (
    <div className="flex min-h-0 min-w-0 flex-1 gap-[clamp(12px,3vw,32px)]">
      {scene.categories.map((category) => {
        const isHover = hover === category.id;
        const isRevealTarget = revealTarget === category.id;
        const inBasket = scene.items.map((item, i) => ({ item, i })).filter(({ i }) => placed[i] === category.id);
        return (
          <button
            key={category.id}
            type="button"
            ref={(el) => {
              basketRefs.current[category.id] = el;
            }}
            onClick={() => onBasketTap(category)}
            aria-label={`Кошик: ${category.label.text}`}
            data-category={category.id}
            className={`relative flex min-h-16 min-w-[max(15vw,64px)] flex-1 cursor-pointer flex-col items-center justify-center gap-[clamp(4px,1.2dvh,10px)] rounded-[clamp(22px,6dvh,40px)] border-[clamp(4px,1dvh,7px)] bg-white/90 p-[clamp(6px,1.6dvh,14px)] shadow-[0_10px_0_var(--color-kid-card-shadow)] transition-transform duration-200 motion-reduce:transition-none ${
              isHover || isRevealTarget ? "border-kid-correct scale-[1.03]" : "border-white"
            } ${selected !== null ? "animate-[breathe_2.4s_ease-in-out_infinite] motion-reduce:animate-none" : ""}`}
          >
            {isRevealTarget ? (
              <span
                aria-hidden="true"
                className="bg-kid-correct absolute -inset-[clamp(6px,1.5dvh,14px)] -z-1 animate-[glowring_1.6s_ease-in-out_infinite] rounded-[48px] motion-reduce:animate-none"
              />
            ) : null}
            <span
              className="w-[clamp(40px,14dvh,96px)] flex-none rounded-2xl"
              style={{ background: stripeFor(category.icon), height: "clamp(32px, 11dvh, 80px)" }}
              role="img"
              aria-label={`Малюнок: ${category.icon}`}
            />
            <span className="text-kid-ink text-[clamp(14px,4dvh,22px)] leading-tight font-semibold">{category.label.text}</span>
            <span className="flex min-h-[clamp(20px,6dvh,44px)] flex-wrap justify-center gap-1.5">
              {inBasket.map(({ item, i }) => (
                <span
                  key={i}
                  className="size-[clamp(20px,6dvh,44px)] animate-[softbounce_400ms_ease-out] rounded-xl border-2 border-white motion-reduce:animate-none"
                  style={{ background: stripeFor(item.icon) }}
                  role="img"
                  aria-label={`Розкладено: ${item.icon}`}
                />
              ))}
            </span>
          </button>
        );
      })}
    </div>
  );

  return (
    <SceneShell
      background={scene.background}
      stage={baskets}
      caption={
        <VoiceLinePlayer
          key={`${caption.line.key}#${caption.n}`}
          line={caption.line}
          variant="caption"
          onReady={() => setCaptionReady(true)}
        />
      }
      hint={done ? undefined : hint}
      onGuideTap={guideTap}
      progress={progress}
    >
      {finished ? (
        <NextButton kind="next" disabled={!captionReady} onClick={onAdvance} />
      ) : (
        <div className="flex w-full items-center justify-center gap-[clamp(12px,2.5vw,24px)]">
          {scene.items.map((item, index) => {
            const isPlaced = placed[index] !== null;
            const isDragging = drag?.index === index;
            const isSelected = selected === index;
            const isRevealed = revealIndex === index;
            return (
              <button
                key={index}
                type="button"
                data-item-index={index}
                aria-label={`Картка ${index + 1}: ${item.icon}`}
                aria-pressed={isSelected}
                // Not `disabled`: a control that becomes disabled under the finger mid-gesture
                // breaks the touch sequence the same way removing it does (see FINISH_DELAY_MS).
                aria-disabled={isPlaced}
                onPointerDown={(e) => onPointerDown(e, index)}
                onPointerMove={onPointerMove}
                onPointerUp={onPointerUp}
                onPointerCancel={onPointerCancel}
                style={isDragging ? { transform: `translate(${drag.dx}px, ${drag.dy}px) scale(1.08)` } : undefined}
                className={`relative size-[clamp(64px,18dvh,120px)] flex-none cursor-grab touch-none rounded-[clamp(16px,4dvh,28px)] border-[clamp(4px,1dvh,7px)] select-none ${
                  isPlaced ? "invisible" : ""
                } ${
                  isDragging
                    ? "z-20 cursor-grabbing shadow-[0_18px_0_var(--color-kid-card-shadow)]"
                    : "transition-transform duration-300 ease-out motion-reduce:transition-none"
                } ${isSelected ? "border-kid-sun -translate-y-2" : isRevealed ? "border-kid-correct" : "border-white"} ${
                  !isDragging && !isSelected && returning !== index ? "animate-[breathe_2.6s_ease-in-out_infinite] motion-reduce:animate-none" : ""
                } shadow-[0_10px_0_var(--color-kid-card-shadow)]`}
              >
                <span className="absolute inset-0 rounded-[inherit]" style={{ background: stripeFor(item.icon) }} />
              </button>
            );
          })}
        </div>
      )}
    </SceneShell>
  );
}
