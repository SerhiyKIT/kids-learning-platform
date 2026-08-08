// Minimal service worker: registers so the app is installable, but does no caching yet.
// Offline support is out of scope for this skeleton (see docs/UX_гайд_дитячого_режиму.md §8 for
// the eventual "lesson continues from cache" requirement this will need to satisfy).

self.addEventListener("install", () => {
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(self.clients.claim());
});
