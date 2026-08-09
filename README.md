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

## Branch status

`main` now holds the modular monolith. The old JHipster microservices setup
(gateway, aiContentService, learningService) has been fully replaced.
