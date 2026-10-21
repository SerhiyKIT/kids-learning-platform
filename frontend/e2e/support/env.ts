// Addresses and TEST-ONLY secrets of the e2e stack (never used outside it; the prod profile can't
// run with "e2e", see ProfileGuard). Ports match infra/e2e/docker-compose.yml.
export const FRONTEND_URL = "http://127.0.0.1:3100";
export const BACKEND_URL = "http://127.0.0.1:18080";
export const MAILPIT_URL = "http://127.0.0.1:58025";

export const E2E_ENV = {
  SERVER_PORT: "18080",
  APP_BASE_URL: FRONTEND_URL,
  ADMIN_BOOTSTRAP_TOKEN: "e2e-bootstrap-token-test-only",
  // 32 zero-padded bytes, base64 — a test value, not a key anything real is encrypted with.
  TWOFA_ENC_KEY: "ZTJlLW9ubHktdGVzdC1rZXktMzItYnl0ZXMtLS0tLS0=",
};
