# Kids Learning Platform

Modular Spring Boot monolith (single PostgreSQL) for a preschool learning
platform. Architecture decisions live in [`/docs`](docs), starting with
[`Технічний_фундамент.md`](docs/Технічний_фундамент.md).

## Layout

- `/backend` — Java 21 / Spring Boot 4.1 monolith
- `/infra` — local Docker infra (PostgreSQL, MinIO)
- `/docs` — architecture and product docs
- `/frontend` — added in a later task

## Run locally

1. `docker compose -f infra/docker-compose.yml up -d`
   (copy `infra/.env.example` to `infra/.env` first and fill in local values)
2. `cd backend && ./mvnw spring-boot:run` (runs with the `dev` profile — see
   `src/main/resources/application-dev.yml`; set `SPRING_PROFILES_ACTIVE=dev`
   if it isn't picked up automatically)

`GET http://localhost:8080/actuator/health` should return `UP` once the app
is up and Flyway has applied its migrations.

## Branch status

This skeleton was built on `feat/monolith-skeleton`. `main` still holds the
old JHipster microservices setup (gateway, aiContentService, learningService)
as a backup until this branch is reviewed and merged.
