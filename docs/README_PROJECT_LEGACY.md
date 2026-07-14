# 🎓 Kids Learning Platform (AI-Powered)

Освітня платформа для дітей з адаптивним навчанням та генерацією контенту за допомогою ШІ (Gemini). Побудована на мікросервісній архітектурі.

## 🏗 Архітектура

Система складається з наступних компонентів:

1.  **Gateway (Port 8080):**
    * Головна точка входу.
    * Frontend (React + TypeScript).
    * Auth (OAuth2 / Keycloak).
2.  **Learning Service (Port 8081):**
    * Управління уроками, предметами, прогресом учнів.
    * База даних: PostgreSQL.
3.  **AI Content Service (Port 8082):**
    * Генерація історій та завдань через Google Gemini API.
    * База даних: MongoDB.
4.  **Інфраструктура:**
    * **JHipster Registry (Port 8761):** Service Discovery & Config Server.
    * **Keycloak (Port 9080):** Identity Provider.

## 🚀 Як запустити проєкт (Швидкий старт)

**Вимоги:**
* Java 21
* Node.js 20+
* Docker Desktop

### Варіант А: Автоматичний запуск (рекомендовано)
У корені проєкту є скрипт запуску.

1.  Надайте права на виконання (тільки перший раз):
    ```bash
    chmod +x start-project.sh
    ```
2.  Запустіть:
    ```bash
    ./start-project.sh
    ```
    *Скрипт автоматично підніме Docker-контейнери, почекає їх завантаження і відкриє 3 окремі вікна терміналу для кожного сервісу.*

### Варіант Б: Ручний запуск

Якщо скрипт не спрацював, виконайте кроки по черзі:

**Крок 1: Запуск інфраструктури (Docker)**
```bash
# З папки gateway
docker compose -f src/main/docker/jhipster-registry.yml up -d
docker compose -f src/main/docker/postgresql.yml up -d
docker compose -f src/main/docker/keycloak.yml up -d

# З папки aiContentService
docker compose -f src/main/docker/mongodb.yml up -d

Важливо: Почекайте 1-2 хвилини, поки завантажиться JHipster Registry (https://www.google.com/search?q=http://localhost:8761).

Крок 2: Запуск сервісів (Java) Відкрийте 3 різні термінали і запустіть:

Термінал 1 (Gateway): cd gateway && ./mvnw

Термінал 2 (Learning): cd learningService && ./mvnw

Термінал 3 (AI Service): cd aiContentService && ./mvnw

🔐 Доступи
Головний сайт: http://localhost:8080

JHipster Registry: http://localhost:8761

User: admin

Pass: admin

Користувачі (Keycloak):

Admin: admin / admin

User: user / user

🛠 Корисні команди
Зупинити все (Docker):

Bash

docker stop $(docker ps -a -q)
Очистити проєкт (якщо є помилки збірки):

Bash

./mvnw clean

---

### Що зробити зараз:

1.  Створи ці два файли.
2.  Надай скрипту права на виконання:
    ```bash
    chmod +x start-project.sh
    ```
3.  Спробуй запустити все однією командою:
    ```bash
    ./start-project.sh
    ```

Це має відкрити тобі 3 нових вікна терміналу, де побіжать логи Maven. Якщо це спрацює — ти повністю готовий до розробки!