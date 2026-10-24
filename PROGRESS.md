# Лог прогресса

## Текущее проверенное состояние

- Корень репозитория: `d:\apps\test_site_harness`
- Стандартный путь старта: `docker compose up -d --build` (app + db) **или** `docker compose up -d` (только db) + `mvnw.cmd spring-boot:run`
- Стандартный путь верификации: `mvnw.cmd test` + `GET http://localhost:8080/actuator/health`
- Текущая незавершённая фича с наивысшим приоритетом: нет (`task` = passing)
- Текущий блокер: нет

## Лог сессий

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
