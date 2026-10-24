# Лог прогресса

## Текущее проверенное состояние

- Корень репозитория: `d:\apps\test_site_harness`
- Стандартный путь старта: `docker compose up -d --build` (app + db) **или** `docker compose up -d` (только db) + `mvnw.cmd spring-boot:run`
- Стандартный путь верификации: `mvnw.cmd test` + `GET http://localhost:8080/actuator/health`
- Текущая незавершённая фича с наивысшим приоритетом: нет (`task` = passing)
- Текущий блокер: нет

## Лог сессий

### Сессия 011

- Дата: 2026-10-24
- Цель: task — ЮKassa payments + payment emulator
- Сделано:
  - Клиент ЮKassa API v3 + stub; таблица `payments`, `orders.payment_status`
  - Checkout создаёт платёж; CRM sale после SUCCEEDED; webhook + refresh
  - UI: статус оплаты / ссылка «Оплатить»
  - `payment-emulator/` (порт 8091) + compose profile `with-payment-emulator`
  - Exec plan: docs/exec-plans/complited/2026-10-24-yookassa-payments.md
- Запущенная верификация:
  - `.\mvnw.cmd test` — shop 34/34 OK
  - `.\mvnw.cmd -f payment-emulator/pom.xml test` — 4/4 OK
  - docker payment-emulator healthy `:8091`; shop health UP
  - E2E live → PENDING → succeed → refresh SUCCEEDED (eventCount=3)
- Зафиксированные доказательства: см. feature-list.json → task.evidence
- Коммиты: (pending)
- Обновлённые файлы или артефакты: src/**/payment/**, payment-emulator/**, docker-compose.yml, feature-list.json, PROGRESS.md, docs/exec-plans/**
- Известный риск или нерешённая проблема:
  - Без YOOKASSA_SHOP_ID / SECRET_KEY — stub mode
  - Эмулятор in-memory; порт 8091; без profile не стартует
  - Корзина не привязана к JWT (только X-Cart-Id)
- Следующий лучший шаг: следующая фича в feature-list.json

### Сессия 010

- Дата: 2026-10-24
- Цель: task — CRM emulator (Spring Boot + optional Docker)
- Сделано:
  - Отдельное приложение `crm-emulator/` (порт 8090): Bitrix24 webhook + amoCRM API v4
  - Inspect API `/emulator/events`, `/emulator/status`; actuator health
  - Docker: `crm-emulator/Dockerfile` + compose profile `with-crm-emulator`
  - Тесты эмулятора 6/6; E2E shop→emulator live SUCCESS
  - Exec plan: docs/exec-plans/complited/2026-10-24-crm-emulator.md
- Запущенная верификация:
  - `.\mvnw.cmd test` — shop 28/28 OK
  - `.\mvnw.cmd -f crm-emulator/pom.xml test` — 6/6 OK
  - docker crm-emulator healthy `:8090`; shop health UP
  - E2E register/favorite/checkout → 6 live SUCCESS + emulator eventCount=6
- Зафиксированные доказательства: см. feature-list.json → task.evidence
- Коммиты: `eea765f`
- Обновлённые файлы или артефакты: crm-emulator/**, docker-compose.yml, feature-list.json, PROGRESS.md, docs/exec-plans/**
- Известный риск или нерешённая проблема:
  - Эмулятор in-memory; порт 8090
  - Без profile `with-crm-emulator` контейнер не стартует (опционально)
  - Корзина не привязана к JWT (только X-Cart-Id)
- Следующий лучший шаг: следующая фича в feature-list.json

### Сессия 009

- Дата: 2026-10-24
- Цель: task — CRM Bitrix24 + amoCRM
- Сделано:
  - Клиенты Bitrix24 (webhook) и amoCRM (API v4); stub без credentials
  - Хуки: register / checkout / favorite → crm_sync_events
  - API: GET /api/crm/status, GET /api/crm/events
  - Liquibase 005-crm-sync; CrmIntegrationTests + client unit tests
  - Exec plan: docs/exec-plans/complited/2026-10-24-crm-integration.md
- Запущенная верификация:
  - docker compose up -d --build — OK; app+db healthy на :8080
  - GET /actuator/health — UP
  - mvnw.cmd test — 28/28 OK
  - Smoke: register/favorite/checkout → 6 SUCCESS (BITRIX24+AMOCRM)
- Зафиксированные доказательства: см. feature-list.json → task.evidence
- Коммиты: `3a3d2b5`
- Обновлённые файлы или артефакты: src/**/crm/**, services, tests, feature-list.json, PROGRESS.md, docs/exec-plans/**
- Известный риск или нерешённая проблема:
  - Без BITRIX24_WEBHOOK_BASE_URL / AMOCRM_* — stub mode
  - Корзина не привязана к JWT (только X-Cart-Id)
  - Порт 8080: конфликт локального run и docker app
- Следующий лучший шаг: следующая фича в feature-list.json

### Сессия 008

- Дата: 2026-10-24
- Цель: task — убрать guest lede с главной
- Сделано:
  - Удалён `<p class="lede">…</p>` из `index.html` (каталог-hero)
  - Assert в `CatalogProfileTests.homePageHasNoTechnicalTokenCopy`
  - Exec plan: `docs/exec-plans/complited/2026-10-24-remove-guest-lede.md`
- Запущенная верификация:
  - `docker compose up -d --build` — OK; app+db healthy на `:8080`
  - `GET /actuator/health` — UP
  - `mvnw.cmd test` — 23/23 OK
  - Smoke: `GET /` без «Смотрите витрину без входа» — OK
- Зафиксированные доказательства: см. `feature-list.json` → task.evidence
- Коммиты: `de37356`
- Обновлённые файлы или артефакты: `index.html`, `CatalogProfileTests.java`, `feature-list.json`, `PROGRESS.md`, `docs/exec-plans/**`
- Известный риск или нерешённая проблема:
  - Корзина не привязана к JWT (только `X-Cart-Id`)
  - Порт 8080: конфликт локального run и docker `app`
- Следующий лучший шаг: следующая фича в `feature-list.json`


### Сессия 007

- Дата: 2026-10-24
- Цель: task — багфиксы logout / избранное / clean URLs
- Сделано:
  - `[hidden]` CSS fix — после выхода только кнопка «Вход»
  - `syncFavoriteButtons` без полной перерисовки каталога
  - `/catalog.html` и `/index.html` → 302 `/`
  - Exec plan: `docs/exec-plans/complited/2026-10-24-bugfixes.md`
- Запущенная верификация:
  - `docker compose up -d --build` — OK; app+db healthy на `:8080`
  - `GET /actuator/health` — UP
  - `mvnw.cmd test` — 23/23 OK
  - Smoke redirects + static JS/CSS — OK
- Зафиксированные доказательства: см. `feature-list.json` → task.evidence
- Коммиты: `4687adb`
- Обновлённые файлы или артефакты: `src/**`, tests, `feature-list.json`, `PROGRESS.md`, `docs/exec-plans/**`
- Известный риск или нерешённая проблема:
  - Корзина не привязана к JWT (только `X-Cart-Id`)
  - Порт 8080: конфликт локального run и docker `app`
- Следующий лучший шаг: следующая фича в `feature-list.json`

### Сессия 006

- Дата: 2026-10-24
- Цель: task — каталог как главная страница; вход в профиль с главной
- Сделано:
  - `index.html` = каталог + панель входа/регистрации (`auth-panel`)
  - `/catalog.html` → редирект на `/`; удалён `auth.js` (логика в `catalog.js`)
  - Тесты: home = catalog + auth chrome
  - Exec plan: `docs/exec-plans/complited/2026-10-24-catalog-as-home.md`
- Запущенная верификация:
  - `docker compose up -d --build` — OK; app+db healthy на `:8080`
  - `GET /actuator/health` — UP
  - `mvnw.cmd test` — 22/22 OK
  - Smoke: `/` product-grid+auth-panel; `/catalog.html` redirect — OK
- Зафиксированные доказательства: см. `feature-list.json` → task.evidence
- Коммиты: `2730469` Make catalog the home page with in-page login.
- Обновлённые файлы или артефакты: `src/main/resources/static/**`, tests, `feature-list.json`, `PROGRESS.md`, `docs/exec-plans/**`
- Известный риск или нерешённая проблема:
  - Корзина не привязана к JWT (только `X-Cart-Id`)
  - Порт 8080: конфликт локального run и docker `app`
- Следующий лучший шаг: следующая фича в `feature-list.json`

### Сессия 005

- Дата: 2026-10-24
- Цель: task — сортировка каталога, qty на карточке, профиль (аватар/избранное/история/выход), убрать тех.инфо
- Сделано:
  - Liquibase `004-catalog-profile.yaml` (brand, avatar_url, favorites, orders/order_items)
  - API: products sort, favorites, orders + checkout; enriched `/api/auth/me`
  - UI: sort toolbar, quantity, profile panel, checkout; index без упоминаний токена
  - `CatalogProfileTests` (7) → всего 22 теста
  - Exec plan: `docs/exec-plans/complited/2026-10-24-catalog-profile.md`
- Запущенная верификация:
  - `docker compose up -d --build` — OK; app+db healthy на `:8080`
  - `GET /actuator/health` — UP
  - `mvnw.cmd test` — 22/22 OK
  - sort/favorites/checkout smoke — OK
- Зафиксированные доказательства: см. `feature-list.json` → task.evidence
- Коммиты: `87f81e3` Add catalog sorting, quantity, and authenticated profile.
- Обновлённые файлы или артефакты: `src/**`, `feature-list.json`, `PROGRESS.md`, `docs/exec-plans/**`
- Известный риск или нерешённая проблема:
  - Корзина не привязана к JWT (только `X-Cart-Id`)
  - Порт 8080: конфликт локального run и docker `app`
- Следующий лучший шаг: следующая фича в `feature-list.json`

### Сессия 004

- Дата: 2026-10-24
- Цель: task — страница товаров и корзина (анонимный доступ)
- Сделано:
  - Liquibase `003-products-cart.yaml` (products + seed, carts, cart_items)
  - API: `GET /api/products`, `GET /api/cart`, `POST /api/cart/items`, `DELETE /api/cart/items/{productId}`
  - UI: `/catalog.html` + CSS/JS; корзина по `X-Cart-Id` в localStorage
  - `CatalogCartTests` (5) + baseline (10) = 15
  - Exec plan: `docs/exec-plans/complited/2026-10-24-products-cart.md`
- Запущенная верификация:
  - `docker compose up -d --build` — OK; app+db healthy на `:8080`
  - `GET /actuator/health` — UP
  - `mvnw.cmd test` — 15/15 OK
  - anonymous products + cart add/remove + `/catalog.html` 200 — OK
- Зафиксированные доказательства: см. `feature-list.json` → task.evidence
- Коммиты: `5453bc4` Add anonymous product catalog and cart (X-Cart-Id).
- Обновлённые файлы или артефакты: `src/**`, `feature-list.json`, `PROGRESS.md`, `docs/exec-plans/**`
- Известный риск или нерешённая проблема:
  - Docker Desktop может зависать при image build — требуется перезапуск
  - Корзина не привязана к JWT-пользователю (только `X-Cart-Id`)
  - Порт 8080: конфликт локального run и docker `app`
- Следующий лучший шаг: следующая фича в `feature-list.json`

### Сессия 003

- Дата: 2026-10-24
- Цель: task — приложение разворачивается в контейнере Docker на порте 8080
- Сделано:
  - `Dockerfile` (multi-stage: `maven:3.9.9-eclipse-temurin-17-alpine` → `eclipse-temurin:17-jre-alpine`)
  - `.dockerignore`
  - Сервис `app` в `docker-compose.yml` (8080, depends_on healthy `db`, env datasource → `db:5432`)
  - Exec plan: `docs/exec-plans/complited/2026-10-24-app-in-docker.md`
- Запущенная верификация:
  - `docker compose up -d --build` — OK; `perfume-shop-app` healthy на `:8080`
  - `GET /actuator/health` — UP
  - `mvnw.cmd test` — 10/10 OK
- Зафиксированные доказательства: см. `feature-list.json` → task.evidence
- Коммиты: `2816330` Add Dockerized Spring Boot app service on port 8080.
- Обновлённые файлы или артефакты: `Dockerfile`, `.dockerignore`, `docker-compose.yml`, `feature-list.json`, `PROGRESS.md`, `docs/exec-plans/**`
- Известный риск или нерешённая проблема:
  - Первый image build долгий (скачивание Maven deps)
  - Порт 8080: конфликт если одновременно локальный `spring-boot:run` и docker `app`
  - Dev credentials БД / JWT secret по-прежнему в compose / yml
- Следующий лучший шаг: следующая фича в `feature-list.json` (каталог/товары)

### Сессия 002

- Дата: 2026-10-24
- Цель: task — страница входа/регистрации, JWT 1 час, сохранение сессии при refresh
- Сделано:
  - Liquibase `002-users.yaml` (таблица `users`)
  - Spring Security + JWT (jjwt), BCrypt пароли
  - API: `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`
  - UI: `static/index.html` + CSS/JS, токен в `localStorage`
  - `AuthFlowTests` (7) + baseline tests (3)
  - Исправлен битый `distributionUrl` Maven Wrapper → Apache Maven 3.9.9
  - Exec plan: `docs/exec-plans/complited/2026-10-24-login-auth.md` → completed
- Запущенная верификация:
  - `docker compose up -d` — OK (container уже Running)
  - `mvnw.cmd test` — 10/10 OK
  - `spring-boot:run` + `/actuator/health` — UP
  - register → me → index.html smoke — OK; `expiresInMs=3600000`
- Зафиксированные доказательства: см. `feature-list.json` → task.evidence
- Коммиты: `c11b1f8` Add login/register with JWT session persistence (1h TTL).
- Обновлённые файлы или артефакты: `pom.xml`, `src/**`, `feature-list.json`, `PROGRESS.md`, `docs/exec-plans/**`, `.mvn/wrapper/maven-wrapper.properties`
- Известный риск или нерешённая проблема:
  - Dev JWT secret захардкожен в `application.yml` (не для prod)
  - Логин нормализуется в lowercase; email/роли/refresh-token не реализованы
  - Системный `mvn` не в PATH — использовать `mvnw.cmd`
- Следующий лучший шаг: следующая фича (каталог/товары) в `feature-list.json`

### Сессия 001

- Дата: 2026-10-24
- Цель: task-001 — PostgreSQL через docker-compose, Liquibase changelog, базовое Spring Boot приложение
- Сделано:
  - Spring Boot 3.3.5 + Maven Wrapper
  - `docker-compose.yml` (PostgreSQL 16)
  - Liquibase master + `001-baseline.yaml`
  - Actuator health + SpringBootTest
  - Временный `002-verification-probe` применён и откатан
- Запущенная верификация:
  - `docker compose up -d` — OK
  - `mvn test` — 3/3 OK (повторно после rollback)
  - `spring-boot:run` + `/actuator/health` — UP
  - `mvn liquibase:rollback -Dliquibase.rollbackCount=1` — OK
- Зафиксированные доказательства: см. историю task-001 (заменена следующей фичей в feature-list)
- Коммиты: `6fb581b` Add Spring Boot baseline with PostgreSQL, Liquibase, and docker-compose (task-001).
- Известный риск или нерешённая проблема:
  - Docker Desktop должен быть запущен до `docker compose up`
  - Локальные credentials БД захардкожены в `application.yml` / compose (только для dev)
