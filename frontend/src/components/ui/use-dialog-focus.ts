"use client";

import { useEffect, useRef, type RefObject } from "react";

const FOCUSABLE = 'a[href], button, input, select, textarea, [tabindex]:not([tabindex="-1"])';

/**
 * Keyboard behaviour of a modal dialog (WCAG 2.1.2 / 2.4.3, QA1-035): focus moves inside on open
 * (an [autofocus] element, else the first focusable one), Tab and Shift+Tab cycle within it,
 * everything else on the page is `inert` (no focus, no clicks, hidden from screen readers), Esc
 * closes it, and focus returns to whatever opened it. The dialog must be rendered straight into
 * <body> (a portal), so that "everything else" is its siblings.
 */
export function useDialogFocus(dialogRef: RefObject<HTMLElement | null>, onClose: () => void): void {
  const onCloseRef = useRef(onClose);
  useEffect(() => {
    onCloseRef.current = onClose;
  }, [onClose]);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    const opener = document.activeElement instanceof HTMLElement ? document.activeElement : null;

    const background = [...document.body.children].filter((el) => !el.contains(dialog) && !el.hasAttribute("inert"));
    background.forEach((el) => el.setAttribute("inert", ""));

    const focusables = () =>
      [...dialog.querySelectorAll<HTMLElement>(FOCUSABLE)].filter((el) => !el.hasAttribute("disabled") && el.getClientRects().length > 0);
    if (!dialog.contains(document.activeElement)) {
      (dialog.querySelector<HTMLElement>("[autofocus]") ?? focusables()[0] ?? dialog).focus();
    }

    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        e.preventDefault();
        onCloseRef.current();
        return;
      }
      if (e.key !== "Tab") return;
      const items = focusables();
      if (items.length === 0) {
        e.preventDefault();
        return;
      }
      const first = items[0];
      const last = items[items.length - 1];
      if (e.shiftKey && (document.activeElement === first || !dialog.contains(document.activeElement))) {
        e.preventDefault();
        last.focus();
      } else if (!e.shiftKey && (document.activeElement === last || !dialog.contains(document.activeElement))) {
        e.preventDefault();
        first.focus();
      }
    };
    document.addEventListener("keydown", onKeyDown);

    return () => {
      document.removeEventListener("keydown", onKeyDown);
      background.forEach((el) => el.removeAttribute("inert"));
      if (opener?.isConnected) opener.focus();
    };
  }, [dialogRef]);
}
