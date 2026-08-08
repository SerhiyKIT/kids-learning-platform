import type { NextConfig } from "next";

// Dev-only same-origin proxying so the session cookie (and the CSRF cookie/header pair) work
// without any cross-origin cookie dance. In prod this is nginx's job (see backend/README or
// infra), not Next.js — this rewrite config only applies to `next dev`/`next start` here.
const BACKEND_ORIGIN = process.env.BACKEND_ORIGIN ?? "http://localhost:8080";

const nextConfig: NextConfig = {
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
