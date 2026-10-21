# End-to-end tests (Playwright)

Critical user journeys against a prod-like stack: the **production** frontend build (standalone
server) and the backend jar with the **`e2e`** profile — not `dev`, so no dev-only endpoints exist;
stub LLM/TTS (no keys); real Postgres, MinIO and Mailpit from `infra/e2e/docker-compose.yml`.

```bash
cd frontend
npm run e2e:deps     # fresh Postgres/MinIO/Mailpit (volumes recreated: the first admin is one-time per DB)
npm run e2e:build    # backend jar + frontend standalone build (BACKEND_ORIGIN=http://127.0.0.1:18080)
npm run test:e2e     # starts backend (:18080) and frontend (:3100), runs everything
```

- `admin.setup.ts` bootstraps the first admin (ADMIN_BOOTSTRAP_TOKEN → 2FA with a TOTP computed by
  `support/totp.ts`) and saves the verified session for the specs that need an admin.
- Every spec creates its own users with unique emails; emails are read from Mailpit's HTTP API.
- `desktop` (1440×900) runs every spec; `phone-landscape` (844×390, touch) the child-mode ones (`@phone`).
- Accessibility: `@axe-core/playwright` fails on serious/critical WCAG A/AA violations on the key
  adult pages; child mode is checked for ≥ 64×64 px targets with an `aria-label` (`support/kid.ts`).
- The secrets in `support/env.ts` are test-only; `prod` refuses to start with the `e2e` profile.
- Contexts run with `reducedMotion: "reduce"`: child mode "breathes" (infinite animations), which
  Playwright would never consider stable enough to click; the app stops them under that setting.

In CI the `e2e` job runs on pull requests only; on failure it uploads traces, screenshots and videos.
