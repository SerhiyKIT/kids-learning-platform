# Kids Learning Platform

Modular Spring Boot monolith (single PostgreSQL) for a preschool learning
platform. Architecture decisions live in [`/docs`](docs), starting with
[`Технічний_фундамент.md`](docs/Технічний_фундамент.md).

## Layout

- `/backend` — Java 21 / Spring Boot 4.1 monolith
- `/frontend` — Next.js (App Router, TypeScript) PWA — adult cabinets only so far; see `frontend/README.md`
- `/infra` — local Docker infra (PostgreSQL, MinIO, Mailpit)
- `/docs` — architecture and product docs

## Run locally

1. `docker compose -f infra/docker-compose.yml up -d`
   (copy `infra/.env.example` to `infra/.env` first and fill in local values)
2. `cd backend && ./mvnw spring-boot:run` (runs with the `dev` profile — see
   `src/main/resources/application-dev.yml`; set `SPRING_PROFILES_ACTIVE=dev`
   if it isn't picked up automatically)

`GET http://localhost:8080/actuator/health` should return `UP` once the app
is up and Flyway has applied its migrations.

Dev emails (verification, password reset) aren't sent to a real inbox — view
them at http://localhost:8025 (Mailpit's web UI).

## Run backend tests

Postgres must be up (see above), and `infra/.env` must be sourced into the
shell first — the `dev` profile's fallback DB password (`kidlearn`) does
**not** match the one actually baked into the Postgres container
(`POSTGRES_PASSWORD` from `infra/.env`), so skipping this step fails nearly
every test with an opaque "Failed to load ApplicationContext" instead of an
obvious auth error:

```
set -a; source infra/.env; set +a
cd backend && ./mvnw test
```

## Creating the first admin account (production)

There's no default/seeded admin login. `POST /api/bootstrap/admin` creates exactly one — the
first — ADMIN account, gated by the `ADMIN_BOOTSTRAP_TOKEN` env var
(`app.bootstrap.admin-token` in `application-prod.yml`):

1. Set `ADMIN_BOOTSTRAP_TOKEN` to a strong random secret at deploy time (unset/blank disables
   the endpoint entirely).
2. `POST /api/bootstrap/admin` with `{token, email, password, displayName}` — matches the
   configured token → creates the admin (201). Wrong token → 401. No token configured → 404.
3. Once any ADMIN exists, the endpoint permanently returns `410 ALREADY_BOOTSTRAPPED`, even with
   the correct token — it only ever creates the first one. `ADMIN_BOOTSTRAP_TOKEN` can then be
   rotated or removed; leaving it set is harmless since the endpoint stays closed.

## Admin two-factor authentication (mandatory)

2FA (TOTP, RFC 6238) is mandatory for every ADMIN account — there's no way to opt out and no
disable endpoint. Set `TWOFA_ENC_KEY` (base64, 32 random bytes, e.g. `openssl rand -base64 32`)
at deploy time — it encrypts TOTP secrets at rest (AES-GCM; see `app.twofa.enc-key` in
`application-prod.yml`) and **must** be set in production, unlike `ADMIN_BOOTSTRAP_TOKEN` above.

After password login, an admin session starts restricted:

- No TOTP set up yet (`totp_enabled_at` unset): only `POST /api/admin/2fa/setup`,
  `POST /api/admin/2fa/enable`, and `GET /api/auth/me` are reachable — every other admin API
  is blocked until setup finishes.
- TOTP already enabled: the fresh session is "pre-2FA" — only `POST /api/admin/2fa/verify`
  (a TOTP code or an unused backup code) is reachable until it succeeds, elevating that session
  for its lifetime.

`POST /api/admin/2fa/enable` returns ~10 backup codes in plaintext exactly once — there's no way
to see them again afterward.

## Branch status

`main` now holds the modular monolith. The old JHipster microservices setup
(gateway, aiContentService, learningService) has been fully replaced.
