"use client";

import { useEffect } from "react";

/** Registers public/sw.js: PWA-installable, and caches voice-line audio only (see that file). */
export function ServiceWorkerRegistration() {
  useEffect(() => {
    if ("serviceWorker" in navigator) {
      navigator.serviceWorker.register("/sw.js").catch(() => {});
    }
  }, []);

  return null;
}
