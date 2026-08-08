"use client";

import { useEffect } from "react";

/** Registers public/sw.js (no-op for now — see that file) so the app is PWA-installable. */
export function ServiceWorkerRegistration() {
  useEffect(() => {
    if ("serviceWorker" in navigator) {
      navigator.serviceWorker.register("/sw.js").catch(() => {});
    }
  }, []);

  return null;
}
