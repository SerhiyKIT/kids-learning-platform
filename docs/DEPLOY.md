# Розгортання пілота на одному VPS

> **Схема:** Інтернет → **nginx** (80 → 301 на 443; TLS від Let's Encrypt) →
> `/api/*` і AJAX `/login`, `/logout` → **backend** (Spring, профіль `prod`); усе інше → **frontend** (Next.js standalone).
> **PostgreSQL** і **MinIO** — лише у внутрішній docker-мережі без виходу в інтернет. Назовні відкриті **тільки 80 і 443**.
> Образи збирає GitHub Actions за git-тегом `v*` і кладе в GHCR; сервер лише `pull` + `up`.

Усе, що потрібно серверу, лежить у `infra/prod/`:

| Файл | Призначення |
|---|---|
| `docker-compose.yml` | nginx, certbot, frontend, backend, postgres, minio |
| `.env.example` | **усі** змінні без значень + як згенерувати кожен секрет |
| `nginx/templates/kidlearn.conf.template` | TLS, проксі, ліміти, закритий `/actuator` |
| `init-letsencrypt.sh` | перше отримання сертифіката |
| `backup/` | бекап, відновлення, systemd-таймер |
| `local/` | **лише** для локальної репетиції (самопідписаний сертифікат) — на сервері не використовується |

---

## 1. Сервер

- **VPS:** ≥ 2 vCPU, 4 GB RAM, 40 GB SSD; Ubuntu Server LTS (24.04).
- **DNS:** A-запис `kids.example.org → <IP сервера>` (і AAAA, якщо є IPv6). Дочекайтеся, поки `dig +short kids.example.org` поверне ваш IP.
- **SSH лише за ключами:** у `/etc/ssh/sshd_config` — `PasswordAuthentication no`, `PermitRootLogin prohibit-password` (або `no`, якщо працюєте через окремого користувача з sudo), потім `systemctl reload ssh`.
- **Firewall — тільки 22/80/443:**
  ```bash
  ufw default deny incoming && ufw default allow outgoing
  ufw allow 22/tcp && ufw allow 80/tcp && ufw allow 443/tcp
  ufw enable
  ```
  Docker сам відкриває лише ті порти, що опубліковані в compose — а це тільки 80/443 nginx. Порти 5432/9000/8080/3000 не публікуються зовсім.
- **Оновлення безпеки:** `apt install unattended-upgrades`.

## 2. Docker

```bash
curl -fsSL https://get.docker.com | sh       # офіційний скрипт: Docker Engine + compose plugin
docker compose version                        # має бути v2+
```

Якщо репозиторій (а отже й пакети GHCR) приватний — увійдіть у GHCR з Personal Access Token з правом **лише `read:packages`**:

```bash
echo "<PAT>" | docker login ghcr.io -u <github-user> --password-stdin
```

## 3. Код і `.env`

```bash
sudo mkdir -p /opt && cd /opt
sudo git clone https://github.com/SerhiyKIT/kids-learning-platform.git
cd kids-learning-platform/infra/prod
sudo cp .env.example .env && sudo chmod 600 .env && sudo nano .env
```

Заповніть **кожну** змінну (підказки — у коментарях `.env.example`). Головне:

- `IMAGE_TAG` — версія для запуску, напр. `v1.0.0` (див. розд. 9).
- `DOMAIN`, `APP_BASE_URL=https://<DOMAIN>`, `LETSENCRYPT_EMAIL`.
- Паролі й ключі генеруйте на місці, напр.:
  ```bash
  openssl rand -base64 32 | tr -d '/+=' | head -c 40   # POSTGRES_PASSWORD, MINIO_ROOT_PASSWORD
  openssl rand -hex 12                                  # MINIO_ROOT_USER
  openssl rand -base64 32                               # TWOFA_ENC_KEY  (збережіть копію в менеджері паролів!)
  openssl rand -hex 32                                  # ADMIN_BOOTSTRAP_TOKEN (одноразовий)
  ```
- Жодних дефолтних паролів: `docker compose` відмовиться стартувати без `IMAGE_TAG`/`DOMAIN`, а бекенд — без будь-якого з обов'язкових секретів.
- `.env` **ніколи** не комітиться (він у `.gitignore`).

## 4. Перший сертифікат

```bash
./init-letsencrypt.sh --staging   # спершу тестовий CA (без лімітів Let's Encrypt), щоб перевірити DNS і порт 80
./init-letsencrypt.sh             # потім справжній сертифікат
```

Скрипт: тимчасовий самопідписаний сертифікат → старт nginx (і всього стеку) → справжній сертифікат через webroot → reload nginx.
Далі все автоматично: контейнер `certbot` двічі на добу виконує `certbot renew`, а nginx перечитує сертифікати кожні 6 годин.

## 5. Перший запуск

```bash
docker compose pull
docker compose up -d
docker compose ps        # усі сервіси "healthy" (backend стартує ~1 хв)
```

Порожній том MinIO — це нормально: бакет створюється сам при першій озвучці.

## 6. Перший адміністратор

1. `ADMIN_BOOTSTRAP_TOKEN` уже в `.env`. Створіть адміна (з вашого комп'ютера):
   ```bash
   curl -sS https://<DOMAIN>/login -c jar -o /dev/null -H 'X-Requested-With: XMLHttpRequest'
   curl -sS https://<DOMAIN>/api/bootstrap/admin -b jar -H "X-XSRF-TOKEN: $(awk '/XSRF-TOKEN/{print $7}' jar)" \
     -H 'Content-Type: application/json' \
     -d '{"token":"<ADMIN_BOOTSTRAP_TOKEN>","email":"admin@…","password":"<≥10 символів>","displayName":"Адміністратор"}'
   rm jar
   ```
   Відповідь `201` — адміна створено; будь-який повторний виклик дасть `410` (ендпоінт закривається назавжди).
2. Увійдіть на `https://<DOMAIN>/login` → вас переведе на налаштування **2FA**: відскануйте QR, введіть код, збережіть резервні коди.
3. **Приберіть токен:** очистіть `ADMIN_BOOTSTRAP_TOKEN=` у `.env` і `docker compose up -d backend`.
4. Далі в адмінці: «Озвучити репліки» (репліки провідника), створення вчителів (запрошення листом).

## 7. Пошта (SMTP)

У `.env`: `SMTP_HOST`, `SMTP_PORT` (587 + `SMTP_STARTTLS=true` або 465 + `SMTP_SSL=true`), `SMTP_USERNAME`, `SMTP_PASSWORD`, `MAIL_FROM` (адреса на вашому домені). Посилання в листах будуються з `APP_BASE_URL` (https).

Щоб листи не потрапляли в спам, у DNS домену `MAIL_FROM`:

- **SPF:** TXT `v=spf1 include:<домен-провайдера> -all` (точне значення дає ваш SMTP-провайдер).
- **DKIM:** TXT/CNAME-записи з ключем, які видає провайдер.
- **DMARC:** TXT на `_dmarc.<домен>`: спершу `v=DMARC1; p=none; rua=mailto:dmarc@<домен>`, після тижня без проблем — `p=quarantine`.

## 8. Що перевірити після деплою (чекліст)

- [ ] `https://<DOMAIN>` відкривається; http → 301 на https.
- [ ] [SSL Labs](https://www.ssllabs.com/ssltest/) — оцінка **A** або **A+** (TLS 1.2/1.3, OCSP stapling).
- [ ] `curl -sI https://<DOMAIN>/login` містить `Strict-Transport-Security`, `Content-Security-Policy`, `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, `Permissions-Policy`.
- [ ] `curl -s -o /dev/null -w '%{http_code}' https://<DOMAIN>/actuator/health` → **404**.
- [ ] `/api/dev/seed-demo`, `/api/dev/register-role` (POST, увійшовши) → **404**.
- [ ] Ззовні закриті 5432, 9000, 8080, 3000: `nmap -p 22,80,443,3000,5432,8080,9000 <DOMAIN>` — відкриті лише 22/80/443.
- [ ] Логін батька, реєстрація з листом-підтвердженням, вхід адміна з 2FA.
- [ ] Rate limit: 6 невдалих входів в один акаунт → 429; додавання власного `X-Forwarded-For` нічого не змінює (nginx його перезаписує).
- [ ] Cookie сесії (DevTools → Application): `Secure`, `HttpOnly`, `SameSite=Lax`.
- [ ] Урок у дитячому режимі зі звуком; `docker compose logs backend` без помилок.

## 9. Оновлення версії

1. Розробник: `git tag v1.2.3 && git push origin v1.2.3` → workflow **Release** збирає обидва образи й публікує `ghcr.io/serhiykit/kids-learning-platform-{backend,frontend}:v1.2.3` (+ `1.2.3`, `latest`).
2. Сервер:
   ```bash
   cd /opt/kids-learning-platform && git pull          # якщо змінились infra/prod-файли
   cd infra/prod && ./backup/backup.sh                 # бекап перед оновленням
   sed -i 's/^IMAGE_TAG=.*/IMAGE_TAG=v1.2.3/' .env
   docker compose pull && docker compose up -d
   ```
   Міграції БД (Flyway) виконуються автоматично при старті бекенда; вони лише додають зміни.
3. Відкат: повернути попередній `IMAGE_TAG` і `docker compose up -d` (якщо нова версія вже змінила схему — відновити бекап, розд. 10).

**MinIO:** офіційні образи MinIO більше не публікуються в Docker Hub/quay.io, тому використовується безкоштовна збірка Chainguard, закріплена за digest. Оновлення: `docker pull cgr.dev/chainguard/minio:latest`, перевірити на тестовій машині, замінити digest у `docker-compose.yml`.

## 10. Бекапи

**Що:** щоденний `pg_dump` (custom format, стиснення) + архів даних MinIO (аудіо). Ротація — 14 днів. **Бекапи містять дані дітей:** тека `/var/backups/kidlearn` — `700`, файли — `600`, лише root.

**Чому systemd-таймер, а не окремий контейнер з cron:** на сервері вже є systemd; таймер не потребує ще одного довгоживучого контейнера з доступом до Docker socket, а пропущений через вимкнення запуск він виконає після старту (`Persistent=true`).

```bash
sudo cp backup/kidlearn-backup.service backup/kidlearn-backup.timer /etc/systemd/system/
# у .service поправте шлях, якщо репозиторій не в /opt/kids-learning-platform
sudo systemctl daemon-reload && sudo systemctl enable --now kidlearn-backup.timer
sudo systemctl start kidlearn-backup.service && ls -l /var/backups/kidlearn   # перший бекап одразу
```

**Шифрування й копія за межами сервера (обов'язково):**

```bash
sudo apt install age
age-keygen -o kidlearn-backup.key        # НА ВАШОМУ КОМП'ЮТЕРІ, не на сервері; збережіть ключ у менеджері паролів
# публічний ключ (рядок "age1…") — у kidlearn-backup.service:
#   Environment=AGE_RECIPIENT=age1...
```

З `AGE_RECIPIENT` скрипт одразу шифрує файли (`*.dump.age`, `*.tar.gz.age`) і не зберігає відкритих копій; розшифрувати може лише власник приватного ключа. Копіюйте зашифровані файли за межі сервера (інший провайдер / S3-сумісне сховище / домашній NAS), напр. `rsync`/`rclone` окремим таймером. Нешифровані бекапи з сервера не виносяться ніколи.

## 11. Відновлення і «тренування відновлення»

```bash
cd /opt/kids-learning-platform/infra/prod
AGE_IDENTITY=/шлях/до/kidlearn-backup.key ./backup/restore.sh /var/backups/kidlearn/db-<час>.dump.age /var/backups/kidlearn/minio-<час>.tar.gz.age
```

Скрипт зупиняє застосунок, замінює БД (`pg_restore --clean`) і дані MinIO, потім запускає все знову. **Це руйнівна операція** — лише для нової машини або свідомого відкату.

**Тренування (раз на квартал, на чистій машині, не на проді):**

1. Нова VM (можна локально), Docker, клон репо, `.env` з **тими самими** `POSTGRES_*`, `MINIO_*`, `TWOFA_ENC_KEY` (без нього адміни не пройдуть 2FA) і `DOMAIN=localhost`.
2. Для localhost — самопідписаний сертифікат і локальні образи замість Let's Encrypt/GHCR, як у репетиції: `local/make-cert.sh`, `docker compose --env-file … -f docker-compose.yml -f local/docker-compose.local.yml up -d` (див. коментар у `local/docker-compose.local.yml`).
3. Скопіювати останній зашифрований бекап, запустити `restore.sh` з `AGE_IDENTITY`.
4. Перевірити: вхід батька, діти й історія на місці, урок відтворює аудіо, вхід адміна з 2FA.
5. Записати дату й тривалість тренування; знищити VM разом із даними.

## 12. Безпека: зроблені рішення й компроміси

- **X-Forwarded-For не підробляється.** nginx **перезаписує** його адресою з'єднання (`$remote_addr`), а бекенд (з `app.ratelimit.trust-forwarded-header: true` лише в prod) бере **останню** адресу ланцюжка — ту, що дописав наш проксі. AJAX `/login`/`/logout` і `/api/*` nginx веде **прямо** на бекенд, а не через rewrite фронтенда: так до бекенда завжди доходить рівно одна адреса. За замовчуванням (`application.yml`) заголовок ігнорується.
- **Профілі не змішуються:** бекенд відмовляється стартувати, якщо поруч із `prod` активний `dev` (dev-ендпоінти, dev-ключ 2FA).
- **Сесія:** `Secure`, `HttpOnly`, `SameSite=Lax`, тайм-аут `SESSION_TIMEOUT` (60 хв): дитячий режим працює в сесії дорослого й підтримує її кожним запитом, тож година покриває заняття з перервами, а забутий пристрій вийде з акаунта того ж дня. CSRF-cookie `XSRF-TOKEN` — `Secure`, `SameSite=Lax`, читається JS навмисно (SPA повертає його заголовком).
- **HSTS:** `max-age` 1 рік **без `includeSubDomains`** — інакше на рік «замкнулися б» усі піддомени, а на них можуть бути сервіси без HTTPS. Коли всі піддомени на HTTPS — можна додати `includeSubDomains` (і `preload`).
- **CSP:** строгий, з nonce на кожен запит (`script-src 'self' 'nonce-…' 'strict-dynamic'`), тож усі сторінки рендеряться на запит (не статично). Компроміс — `style-src-attr 'unsafe-inline'`: React рендерить атрибути `style=""` (палітра теми, розміри); елементи `<style>` усе одно потребують nonce. Щоб прибрати і це, треба винести динамічні стилі з атрибутів у CSS-змінні через класи.
- **Actuator:** лише `health`/`info` без деталей, nginx на `/actuator` відповідає 404; healthcheck контейнера ходить напряму всередині контейнера.
- **Логи:** без паролів, токенів, ключів і тіл запитів; помилки відправки листів логуються за id користувача, не за email.

## 13. Строки зберігання (автоочищення)

Щоночі (`RETENTION_CRON`, за замовчуванням `0 30 3 * * *`) бекенд видаляє: відповіді по сценах старші за 2 роки (`RETENTION_DETAILED_HISTORY`, `P2Y`; підсумки уроків з оцінками лишаються), використані/прострочені посилання з листів старші за 30 днів, акаунти батьків без підтвердженого email старші за 30 днів (у них не може бути дітей). У лог пишуться лише кількості. Розподіленого локу немає — пілот працює **одним** екземпляром бекенда; перед масштабуванням на кілька екземплярів додати лок (напр. ShedLock).

## 14. Відомі обмеження пілота

- **Немає сторінок `/verify-email` і `/reset-password` на фронтенді.** Листи ведуть на ці адреси, і бекенд їх обслуговує (`POST /api/auth/verify-email`, `/reset-password`), але UI відсутній. Без підтвердження email батько не може створити профіль дитини. **Потрібно додати ці сторінки до запуску пілота.**
- Один сервер = одна точка відмови; RPO — до доби (щоденний бекап).
