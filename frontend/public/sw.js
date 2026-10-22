// Service worker: makes the app installable and caches two things — voice-line audio, and one
// static page (/offline.html, "ask a grown-up") shown when a page can't be loaded at all (QA1-028).
//
// GET /api/audio/{textHash} is content-addressed (the hash names the exact bytes), so a cached
// copy can never go stale: cache-first, kept in its own versioned cache. NOTHING else under /api
// is ever cached or even intercepted — those responses carry children's data — and non-GET
// requests (login, logout, CSRF-protected POSTs) are never touched.

const AUDIO_CACHE = "kid-audio-v1";
// The offline page holds no user data: a picture, one line for the adult, no script.
const OFFLINE_CACHE = "kid-offline-v1";
const OFFLINE_PAGE = "/offline.html";
const KEEP_CACHES = [AUDIO_CACHE, OFFLINE_CACHE];
const MAX_AUDIO_ENTRIES = 300;
const AUDIO_PATH = /^\/api\/audio\/[0-9a-f]{64}$/;

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches
      .open(OFFLINE_CACHE)
      .then((cache) => cache.add(OFFLINE_PAGE))
      .then(() => self.skipWaiting()),
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((names) => Promise.all(names.filter((name) => !KEEP_CACHES.includes(name)).map((name) => caches.delete(name))))
      .then(() => self.clients.claim()),
  );
});

self.addEventListener("fetch", (event) => {
  const request = event.request;
  // Page loads go to the network as always — nothing is stored; only when that fails outright
  // (no connection) does the cached offline picture stand in for the page.
  if (request.mode === "navigate") {
    event.respondWith(fetch(request).catch(() => caches.match(OFFLINE_PAGE, { cacheName: OFFLINE_CACHE })));
    return;
  }
  if (request.method !== "GET" || request.headers.has("range")) return;
  const url = new URL(request.url);
  if (url.origin !== self.location.origin || !AUDIO_PATH.test(url.pathname)) return;
  event.respondWith(cachedAudio(request));
});

async function cachedAudio(request) {
  const cache = await caches.open(AUDIO_CACHE);
  const hit = await cache.match(request);
  if (hit) {
    // Simple LRU: re-inserting moves the entry to the end of the cache's key order.
    await cache.delete(request);
    await cache.put(request, hit.clone());
    return hit;
  }
  const response = await fetch(request);
  // Only full, successful responses: never a 401/404, a partial 206 or an opaque response.
  if (response.ok && response.status === 200 && response.type === "basic") {
    await cache.put(request, response.clone());
    await trim(cache);
  }
  return response;
}

async function trim(cache) {
  const keys = await cache.keys();
  for (let i = 0; i < keys.length - MAX_AUDIO_ENTRIES; i += 1) {
    await cache.delete(keys[i]);
  }
}
