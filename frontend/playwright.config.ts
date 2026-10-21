import { defineConfig, devices } from "@playwright/test";
import { E2E_ENV, FRONTEND_URL, BACKEND_URL } from "./e2e/support/env";

/**
 * End-to-end tests against a prod-like stack (frontend/e2e/README.md):
 * - dependencies from infra/e2e/docker-compose.yml (Postgres, MinIO, Mailpit), started beforehand;
 * - the backend jar with the "e2e" profile (not dev: no dev-only endpoints) and the frontend's
 *   production standalone build, both started here.
 * Projects: "setup" bootstraps the first admin once (the endpoint is one-time per database);
 * "desktop" runs every spec, "phone-landscape" the child-mode ones (tagged @phone).
 */
export default defineConfig({
  testDir: "./e2e",
  timeout: 120_000,
  expect: { timeout: 15_000 },
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [["list"], ["html", { open: "never" }]] : [["list"]],
  outputDir: "test-results",
  use: {
    baseURL: FRONTEND_URL,
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    video: "retain-on-failure",
    locale: "uk-UA",
    // Child mode "breathes" (infinite CSS animations), so its targets are never "stable" for
    // Playwright's actionability check. The app honours prefers-reduced-motion and stops them.
    reducedMotion: "reduce",
  },
  projects: [
    { name: "setup", testMatch: /admin\.setup\.ts/ },
    {
      name: "desktop",
      dependencies: ["setup"],
      use: { ...devices["Desktop Chrome"], viewport: { width: 1440, height: 900 } },
    },
    {
      name: "phone-landscape",
      dependencies: ["setup"],
      grep: /@phone/,
      use: {
        ...devices["Desktop Chrome"],
        viewport: { width: 844, height: 390 },
        isMobile: true,
        hasTouch: true,
      },
    },
  ],
  webServer: [
    {
      command: "java -jar ../backend/target/backend-0.0.1-SNAPSHOT.jar",
      url: `${BACKEND_URL}/actuator/health`,
      env: { ...E2E_ENV, SPRING_PROFILES_ACTIVE: "e2e" },
      timeout: 180_000,
      reuseExistingServer: !process.env.CI,
      stdout: "ignore",
      stderr: "pipe",
    },
    {
      command: "node .next/standalone/server.js",
      url: `${FRONTEND_URL}/login`,
      env: { PORT: "3100", HOSTNAME: "127.0.0.1", NODE_ENV: "production" },
      timeout: 60_000,
      reuseExistingServer: !process.env.CI,
    },
  ],
});
