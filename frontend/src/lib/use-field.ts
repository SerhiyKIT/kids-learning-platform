"use client";

import { useCallback, useEffect, useRef, useState, useSyncExternalStore, type ChangeEvent } from "react";

/**
 * A text field of a public form that keeps what was typed before React hydrated the page
 * (QA1-003). A controlled input (`value={state}`) is reset to the initial state when React takes
 * over the server-rendered HTML, so on a slow phone the first characters vanish. This field is
 * uncontrolled — React never writes its DOM value — and its state is read back from the DOM right
 * after hydration, then kept in step by onChange. `set` changes both (e.g. clearing a password).
 */
export function useField() {
  const ref = useRef<HTMLInputElement>(null);
  const [value, setValue] = useState("");

  useEffect(() => {
    const typedBeforeHydration = ref.current?.value ?? "";
    if (typedBeforeHydration !== "") setValue(typedBeforeHydration);
  }, []);

  const set = useCallback((next: string) => {
    setValue(next);
    if (ref.current) ref.current.value = next;
  }, []);

  const onChange = useCallback((e: ChangeEvent<HTMLInputElement>) => setValue(e.target.value), []);

  return { value, set, inputProps: { ref, defaultValue: "", onChange } };
}

const noSubscription = () => () => {};

/** False in the server-rendered HTML and until hydration, then true: for actions without fields. */
export function useHydrated(): boolean {
  return useSyncExternalStore(
    noSubscription,
    () => true,
    () => false,
  );
}
