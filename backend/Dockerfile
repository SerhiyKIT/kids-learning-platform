# syntax=docker/dockerfile:1
# Production image of the Spring Boot backend: build with the Maven wrapper on a JDK, run only the
# jar on a JRE as an unprivileged user. No secrets in the image — every one comes from env at run
# time (application-prod.yml has no defaults for them).
#
# Build from the REPOSITORY ROOT (`docker build -f backend/Dockerfile .`): the jar embeds
# docs/lesson-scenario.schema.json and docs/Промпт_генерації_уроків.md, whose single copy lives in
# docs/ (see pom.xml). backend/Dockerfile.dockerignore limits the context to backend/ and those two.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /src/backend
COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -B dependency:go-offline
COPY docs/lesson-scenario.schema.json docs/Промпт_генерації_уроків.md /src/docs/
COPY backend/src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -B -DskipTests package \
    && cp target/backend-*.jar /src/app.jar

FROM eclipse-temurin:21-jre
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
# Owned by root and read-only for the app user: the process can't modify its own code.
COPY --from=build /src/app.jar /app/app.jar
USER app
# Container-aware heap: 75% of the memory limit, and die (to be restarted) rather than limp on
# after an OutOfMemoryError. Override JAVA_OPTS to tune.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError" \
    SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=5 \
    CMD curl -fsS http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
