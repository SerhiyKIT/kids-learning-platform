# ── Stage 1: Build frontend + backend ────────────────────────────────────────
FROM eclipse-temurin:21-jdk-jammy AS build

# Install Node.js 20
RUN apt-get update && apt-get install -y curl ca-certificates gnupg && \
    curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y nodejs && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Cache Maven dependencies
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -ntp -q

# Cache npm dependencies
COPY package.json package-lock.json ./
RUN npm ci --prefer-offline --no-audit

# Copy the rest of the project (configs, src, webpack — node_modules/target are
# excluded via .dockerignore)
COPY . .

# Build frontend assets → target/classes/static/
RUN npm run webapp:prod

# Compile Java + package JAR (skip frontend-maven-plugin, assets already built)
RUN ./mvnw -ntp -Pprod package -DskipTests -Dskip.installnodenpm -Dskip.npm

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

RUN apt-get update && apt-get install -y curl && apt-get clean && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
