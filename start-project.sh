#!/bin/bash

# --- Налаштування кольорів для виводу ---
GREEN='\033[0;32m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${CYAN}=== Запуск Kids Learning Platform ===${NC}"

# 1. Запуск Інфраструктури (Docker)
echo -e "${GREEN}[1/4] Запускаємо бази даних та сервіси (Docker)...${NC}"

# Запускаємо Registry (з Gateway папки, бо ми там його налаштували)
cd gateway
docker compose -f src/main/docker/jhipster-registry.yml up -d
docker compose -f src/main/docker/postgresql.yml up -d
docker compose -f src/main/docker/keycloak.yml up -d
cd ..

# Запускаємо MongoDB для AI сервісу
cd aiContentService
docker compose -f src/main/docker/mongodb.yml up -d
cd ..

# 2. Очікування готовності
echo -e "${GREEN}[2/4] Чекаємо 40 секунд на ініціалізацію JHipster Registry та Keycloak...${NC}"
echo -e "${CYAN}(Можна поки піти зробити каву ☕)${NC}"

sleep 40

# 3. Запуск Java додатків
echo -e "${GREEN}[3/4] Відкриваємо термінали для мікросервісів...${NC}"

# Функція для відкриття нового терміналу в Ubuntu
launch_service() {
    service_name=$1
    folder=$2
    # Відкриває нове вікно gnome-terminal, заходить в папку і запускає maven
    gnome-terminal --title="$service_name" -- bash -c "cd $folder; echo 'Запуск $service_name...'; ./mvnw; exec bash"
}

launch_service "GATEWAY (Frontend + Backend)" "gateway"
launch_service "LEARNING SERVICE" "learningService"
launch_service "AI CONTENT SERVICE" "aiContentService"

# 4. Фінал
echo -e "${GREEN}[4/4] Всі команди відправлено!${NC}"
echo -e "Відкрий браузер: ${CYAN}http://localhost:8080${NC}"
echo -e "Адмінка Registry: ${CYAN}http://localhost:8761${NC}"