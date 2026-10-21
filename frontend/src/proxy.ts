import { NextResponse, type NextRequest } from "next/server";

/**
 * Per-request Content-Security-Policy with a nonce (node_modules/next/dist/docs/01-app/02-guides/
 * content-security-policy.md): Next puts the nonce on its own scripts and inline styles, and
 * 'strict-dynamic' lets those load the rest — no 'unsafe-inline' for scripts. Every page is
 * rendered per request for this (see the root layout).
 *
 * What else the app needs, and nothing more:
 * - img-src blob: data: — the admin's 2FA QR code is a data: URL generated in the browser;
 * - media-src blob: data: — voice lines play from blob: URLs, the unlock clip is a data: WAV;
 * - worker-src 'self' — the audio service worker /sw.js;
 * - style-src-attr 'unsafe-inline' — React renders style="" attributes (theme palette, sizes);
 *   only attributes, not <style> elements, which still need the nonce. Compromise documented in
 *   docs/DEPLOY.md.
 * Fonts are self-hosted by next/font, so font-src 'self'.
 */
export function proxy(request: NextRequest) {
  const nonce = Buffer.from(crypto.randomUUID()).toString("base64");
  const isDev = process.env.NODE_ENV === "development";
  const csp = [
    "default-src 'self'",
    `script-src 'self' 'nonce-${nonce}' 'strict-dynamic'${isDev ? " 'unsafe-eval'" : ""}`,
    // Dev tooling injects un-nonced <style>; with a nonce present 'unsafe-inline' would be ignored.
    isDev ? "style-src 'self' 'unsafe-inline'" : `style-src 'self' 'nonce-${nonce}'`,
    "style-src-attr 'unsafe-inline'",
    "img-src 'self' blob: data:",
    "media-src 'self' blob: data:",
    "font-src 'self'",
    `connect-src 'self'${isDev ? " ws:" : ""}`,
    "worker-src 'self'",
    "manifest-src 'self'",
    "object-src 'none'",
    "base-uri 'self'",
    "form-action 'self'",
    "frame-ancestors 'none'",
    ...(isDev ? [] : ["upgrade-insecure-requests"]),
  ].join("; ");

  const requestHeaders = new Headers(request.headers);
  requestHeaders.set("x-nonce", nonce);
  requestHeaders.set("Content-Security-Policy", csp);
  const response = NextResponse.next({ request: { headers: requestHeaders } });
  response.headers.set("Content-Security-Policy", csp);
  return response;
}

export const config = {
  matcher: [
    {
      // Pages (the /login page included): not /api, static chunks, the service worker or icons.
      source: "/((?!api/|_next/static|_next/image|sw.js|favicon.ico|icon.svg|manifest.webmanifest).*)",
      missing: [
        { type: "header", key: "next-router-prefetch" },
        { type: "header", key: "purpose", value: "prefetch" },
      ],
    },
  ],
};
