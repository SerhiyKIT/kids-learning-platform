# frontend

Next.js (App Router, TypeScript) PWA skeleton for the adult-facing cabinets (parent, teacher,
admin). Child mode / scene engine is a later task — see
`../docs/UX_гайд_дитячого_режиму.md`.

## Auth plumbing

Auth is the backend's httpOnly session cookie — this app never stores a token itself. `src/lib/api.ts`:

- `apiFetch(path, init)` — for `/api/*` JSON endpoints. Always sends `credentials: "include"`;
  for non-GET requests it reads the readable `XSRF-TOKEN` cookie and echoes it as
  `X-XSRF-TOKEN` (the backend uses `CookieCsrfTokenRepository.withHttpOnlyFalse()`). Throws a
  typed `ApiError` (`status`, optional `code`/`problems`/`retryAfterSeconds`) on non-2xx.
- `loginWithFormPost` / `logout` — Spring Security's form-login/logout endpoints respond with a
  redirect, not JSON, so these don't reuse `apiFetch`; see the comment in that file for how
  success/failure is told apart.
- `ensureCsrfCookie()` — fires a throwaway GET so the `XSRF-TOKEN` cookie exists before the very
  first POST in a fresh session (the backend only sets it as a side effect of a request going
  through its filter chain).

## Dev same-origin proxying

`next.config.ts` rewrites `/api/:path*`, `/login`, and `/logout` to `BACKEND_ORIGIN` (default
`http://localhost:8080`) so the session/CSRF cookies work without any cross-origin dance. Copy
`.env.example` to `.env.local` to override it. **This rewrite setup is dev-only** — in production
that routing is nginx's job (see the root README / `/infra`), not this file.

## Run locally

1. Start the backend (`docker compose -f ../infra/docker-compose.yml up -d` +
   `cd ../backend && ./mvnw spring-boot:run`, see the root README).
2. `npm install`
3. `npm run dev` — http://localhost:3000

## PWA

`public/manifest.webmanifest` + `public/sw.js` (registers, does no caching yet) make the app
installable. Offline lesson caching is out of scope here — see the UX guide §8/§9 for what that
will eventually need to satisfy.

## Safari / WebKit against a production build

A production build (`NODE_ENV=production`, the standalone server, the e2e stack) sends a CSP with
`upgrade-insecure-requests`. WebKit applies it even on `http://127.0.0.1`, so every script and API
request is upgraded to `https://` and fails: the page never hydrates and no CSRF cookie appears.
Chromium and Firefox treat loopback as secure and don't upgrade. Production is HTTPS only, so this
is expected, and the directive stays (QA1-002 — deliberately not changed): to look at a production
build in Safari/WebKit locally, put a TLS proxy in front of it (any self-signed one, e.g. a few lines
of `node:https` forwarding to `127.0.0.1:3100`) and open `https://127.0.0.1:<proxy port>`. The dev
server (`npm run dev`) doesn't send the directive and works in Safari as is.
