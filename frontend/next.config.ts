import type { NextConfig } from "next";

// Same-origin proxying so the session cookie (and the CSRF cookie/header pair) work without any
// cross-origin cookie dance. In production nginx already routes /api/* and the AJAX /login and
// /logout straight to the backend (infra/prod/nginx), so these only matter in dev and as a
// fallback. Rewrites are compiled at `next build`: BACKEND_ORIGIN is a build-time value (the
// frontend Dockerfile passes http://backend:8080).
const BACKEND_ORIGIN = process.env.BACKEND_ORIGIN ?? "http://localhost:8080";

// Security headers for every response the frontend serves. The Content-Security-Policy itself is
// per request (it carries a nonce) and is set in src/proxy.ts. HSTS deliberately has no
// includeSubDomains: other subdomains of the pilot's domain may not serve HTTPS, and the flag
// would lock browsers out of them for a year (docs/DEPLOY.md). Browsers ignore HSTS over plain
// http, so it is harmless in local dev.
const SECURITY_HEADERS = [
  { key: "Strict-Transport-Security", value: "max-age=31536000" },
  { key: "X-Content-Type-Options", value: "nosniff" },
  { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
  { key: "X-Frame-Options", value: "DENY" },
  { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=(), payment=()" },
];

const nextConfig: NextConfig = {
  // Self-contained server (server.js + traced node_modules) for the production image.
  output: "standalone",
  poweredByHeader: false,
  async headers() {
    return [{ source: "/:path*", headers: SECURITY_HEADERS }];
  },
  async rewrites() {
    return {
      // Our own /login and /logout pages/GETs must win for plain browser navigation, but a
      // plain (afterFiles) rewrite never even gets tried: App Router pages respond to every
      // HTTP method, not just GET, so a POST to /login would otherwise render the login page's
      // HTML instead of reaching the backend. src/lib/api.ts's loginWithFormPost/logout send
      // X-Requested-With so only THAT AJAX call is routed to the backend here, before Next's own
      // page matching runs; a real GET navigation never sends that header.
      beforeFiles: [
        {
          source: "/login",
          has: [{ type: "header", key: "x-requested-with", value: "XMLHttpRequest" }],
          destination: `${BACKEND_ORIGIN}/login`,
        },
        {
          source: "/logout",
          has: [{ type: "header", key: "x-requested-with", value: "XMLHttpRequest" }],
          destination: `${BACKEND_ORIGIN}/logout`,
        },
      ],
      afterFiles: [{ source: "/api/:path*", destination: `${BACKEND_ORIGIN}/api/:path*` }],
    };
  },
};

export default nextConfig;
