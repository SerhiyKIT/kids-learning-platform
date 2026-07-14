#!/bin/bash

# Kids Learning Platform — unified launcher
# Modes:
#   ./start-project.sh            → dev mode (Docker infra + Maven in terminals)
#   ./start-project.sh docker     → full Docker mode (docker compose up)
#   ./start-project.sh down       → stop everything

GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

case "$1" in

  # ── Full Docker mode ──────────────────────────────────────────────────────
  docker)
    echo -e "${CYAN}=== Kids Learning Platform — Docker mode ===${NC}"
    echo -e "${YELLOW}Збираємо та запускаємо всі сервіси через docker compose...${NC}"
    echo ""
    echo -e "${YELLOW}⚠  Перша збірка займає 5–10 хвилин (завантаження Maven/npm залежностей)${NC}"
    echo ""

    if [ -n "$GEMINI_API_KEY" ]; then
      echo -e "${GREEN}✓ GEMINI_API_KEY знайдено${NC}"
    else
      echo -e "${YELLOW}ℹ GEMINI_API_KEY не встановлено — AI підказки працюватимуть у fallback-режимі${NC}"
    fi

    cd "$ROOT_DIR"
    docker compose up --build -d

    echo ""
    echo -e "${GREEN}=== Сервіси запускаються ===${NC}"
    echo -e "Слідкуй за логами:   ${CYAN}docker compose logs -f gateway${NC}"
    echo -e "Зупинити всі:        ${CYAN}docker compose down${NC}"
    echo -e "Зупинити + видалити: ${CYAN}docker compose down -v${NC}"
    echo ""
    echo -e "Після запуску:"
    echo -e "  Застосунок:  ${CYAN}http://localhost:8080${NC}"
    echo -e "  Eureka:      ${CYAN}http://localhost:8761${NC}  (admin / admin)"
    echo -e "  Keycloak:    ${CYAN}http://localhost:9080${NC}  (admin / admin)"
    ;;

  # ── Stop everything ───────────────────────────────────────────────────────
  down)
    echo -e "${CYAN}=== Зупинка всіх контейнерів ===${NC}"
    cd "$ROOT_DIR"
    docker compose down
    echo -e "${GREEN}Зупинено.${NC}"
    ;;

  # ── Dev mode (default) ────────────────────────────────────────────────────
  *)
    echo -e "${CYAN}=== Kids Learning Platform — Dev mode ===${NC}"

    echo -e "${GREEN}[1/4] Запускаємо інфраструктуру (Docker)...${NC}"
    cd "$ROOT_DIR"
    docker compose up -d keycloak jhipster-registry postgres-gateway postgres-learning mongodb
    echo ""

    echo -e "${GREEN}[2/4] Очікуємо готовності Keycloak та Registry...${NC}"
    echo -e "${YELLOW}Зазвичай займає ~40 секунд...${NC}"

    until docker compose exec keycloak curl -sf http://localhost:9080/health/ready > /dev/null 2>&1; do
      printf "."
      sleep 3
    done
    echo ""
    echo -e "${GREEN}✓ Keycloak готовий${NC}"

    until docker compose exec jhipster-registry curl -sf http://localhost:8761/management/health > /dev/null 2>&1; do
      printf "."
      sleep 3
    done
    echo ""
    echo -e "${GREEN}✓ JHipster Registry готовий${NC}"

    echo ""
    echo -e "${GREEN}[3/4] Відкриваємо термінали для мікросервісів...${NC}"

    launch_service() {
      local name="$1"
      local dir="$2"
      local cmd="$3"
      gnome-terminal --title="$name" -- bash -c "cd '$dir'; echo 'Запуск $name...'; $cmd; exec bash" 2>/dev/null || \
      xterm -title "$name" -e "bash -c \"cd '$dir'; $cmd; exec bash\"" 2>/dev/null || \
      (echo -e "${YELLOW}Відкрийте новий термінал і виконайте в $dir:${NC} $cmd")
    }

    launch_service "GATEWAY" "$ROOT_DIR/gateway" "./mvnw -Dskip.installnodenpm -Dskip.npm"
    launch_service "LEARNING SERVICE" "$ROOT_DIR/learningService" "./mvnw"
    launch_service "AI SERVICE" "$ROOT_DIR/aiContentService" "./mvnw"

    echo ""
    echo -e "${GREEN}[4/4] Запускаємо фронтенд...${NC}"
    gnome-terminal --title="FRONTEND (npm)" -- bash -c "cd '$ROOT_DIR/gateway'; npm start; exec bash" 2>/dev/null || \
      echo -e "${YELLOW}Відкрийте новий термінал і виконайте:${NC} cd gateway && npm start"

    echo ""
    echo -e "${GREEN}=== Готово! ===${NC}"
    echo -e "  Фронтенд:   ${CYAN}http://localhost:9000${NC}"
    echo -e "  API:        ${CYAN}http://localhost:8080${NC}"
    echo -e "  Eureka:     ${CYAN}http://localhost:8761${NC}  (admin / admin)"
    echo -e "  Keycloak:   ${CYAN}http://localhost:9080${NC}  (admin / admin)"
    echo ""
    echo -e "  Тестовий юзер: ${YELLOW}user / user${NC}"
    echo -e "  Адмін:        ${YELLOW}admin / admin${NC}"
    ;;
esac
