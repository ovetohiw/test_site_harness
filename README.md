# Perfume Shop

Онлайн-магазин парфюмерии на Spring Boot: каталог, корзина, регистрация/вход (JWT), избранное, заказы, оплата через ЮKassa и синхронизация с CRM (Bitrix24 + amoCRM).

## Стек

| Слой | Технологии |
|------|------------|
| Backend | Java 17, Spring Boot 3.3.5, Spring Security, JPA |
| БД | PostgreSQL 16, Liquibase |
| Сборка | Maven Wrapper (`mvnw` / `mvnw.cmd`) |
| Frontend | HTML, CSS, JavaScript (static) |
| Инфра | Docker Compose |
| Платежи | ЮKassa API v3 (+ локальный эмулятор) |
| CRM | Bitrix24 webhook, amoCRM API v4 (+ локальный эмулятор) |

## Возможности

- Каталог товаров на главной (`/`) с сортировкой и количеством
- Анонимная корзина по заголовку `X-Cart-Id`
- Регистрация / вход, JWT-сессия (1 час), профиль
- Избранное и история заказов
- Checkout → платёж ЮKassa (или stub без credentials)
- CRM-события: регистрация, избранное, продажа после успешной оплаты
- Health-check: `GET /actuator/health`

## Быстрый старт

Нужны Docker Desktop и JDK 17 (для локального запуска/тестов).

### Вариант A — всё в Docker

```bash
docker compose up -d --build
```

Приложение: http://localhost:8080  
БД: `localhost:5432` (`perfume` / `perfume` / `perfume_shop`)

### Вариант B — БД в Docker, приложение локально

```bash
docker compose up -d db
./mvnw spring-boot:run
# Windows: .\mvnw.cmd spring-boot:run
```

Не запускайте одновременно локальный `spring-boot:run` и контейнер `app` — оба занимают порт `8080`.

### Проверка

```bash
curl http://localhost:8080/actuator/health
./mvnw test
# Windows: .\mvnw.cmd test
```

Ожидается `"status":"UP"` и зелёные тесты.

## Опциональные эмуляторы

Эмуляторы поднимаются через Compose-профили и не стартуют по умолчанию.

### CRM (порт 8090)

```bash
docker compose --profile with-crm-emulator up -d --build crm-emulator
```

Переменные для магазина (та же Docker-сеть):

```text
BITRIX24_WEBHOOK_BASE_URL=http://crm-emulator:8090/rest/1/test/
AMOCRM_BASE_URL=http://crm-emulator:8090
AMOCRM_ACCESS_TOKEN=test-token
```

Inspect: `GET http://localhost:8090/emulator/events`, `GET http://localhost:8090/emulator/status`

### ЮKassa (порт 8091)

```bash
docker compose --profile with-payment-emulator up -d --build payment-emulator
```

```text
YOOKASSA_BASE_URL=http://payment-emulator:8091/v3
YOOKASSA_SHOP_ID=test-shop
YOOKASSA_SECRET_KEY=test-secret
YOOKASSA_RETURN_URL=http://localhost:8080/
```

Без `YOOKASSA_SHOP_ID` / `YOOKASSA_SECRET_KEY` магазин работает в **stub mode** (локальный `SUCCEEDED` без HTTP).  
Без CRM credentials — аналогично stub для Bitrix24/amoCRM.

Тесты эмуляторов:

```bash
./mvnw -f crm-emulator/pom.xml test
./mvnw -f payment-emulator/pom.xml test
```

## Структура репозитория

```text
├── src/main/java/com/perfumeshop/   # магазин (domain, dto, service, web, payment, crm, security)
├── src/main/resources/
│   ├── static/                      # UI (HTML/CSS/JS)
│   ├── db/changelog/                # Liquibase
│   └── application.yml
├── crm-emulator/                    # эмулятор Bitrix24 + amoCRM
├── payment-emulator/                # эмулятор ЮKassa
├── docs/exec-plans/                 # планы фич (active / completed)
├── docker-compose.yml
├── feature-list.json                # статус фич и evidence
├── PROGRESS.md                      # лог сессий
├── PLANS.md                         # правила exec plans
└── AGENTS.md                        # правила для агентов
```

## Основные API

| Метод | Путь | Описание |
|-------|------|----------|
| `POST` | `/api/auth/register` | Регистрация |
| `POST` | `/api/auth/login` | Вход |
| `GET` | `/api/auth/me` | Текущий пользователь (JWT) |
| `GET` | `/api/products` | Каталог (сортировка) |
| `GET` | `/api/cart` | Корзина (`X-Cart-Id`) |
| `POST` | `/api/cart/items` | Добавить в корзину |
| `DELETE` | `/api/cart/items/{productId}` | Убрать из корзины |
| `POST` | `/api/favorites` | Избранное (JWT) |
| `POST` | `/api/orders/checkout` | Оформление заказа + платёж |
| `GET` | `/api/payments` | Платежи пользователя |
| `GET` | `/api/payments/status` | Статус оплаты |
| `POST` | `/api/payments/{id}/refresh` | Обновить статус из ЮKassa |
| `POST` | `/api/payments/webhook` | Webhook ЮKassa |
| `GET` | `/api/crm/status` | Статус CRM-провайдеров |
| `GET` | `/api/crm/events` | Журнал CRM-синхронизации |

## Конфигурация

Ключевые переменные окружения (см. `application.yml` и `docker-compose.yml`):

| Переменная | Назначение |
|------------|------------|
| `SPRING_DATASOURCE_*` | JDBC к PostgreSQL |
| `BITRIX24_WEBHOOK_BASE_URL` | Bitrix24 inbound webhook |
| `AMOCRM_BASE_URL` / `AMOCRM_ACCESS_TOKEN` | amoCRM |
| `YOOKASSA_BASE_URL` | База API ЮKassa (по умолчанию prod; для эмулятора — `…/v3`) |
| `YOOKASSA_SHOP_ID` / `YOOKASSA_SECRET_KEY` | Учётные данные ЮKassa |
| `YOOKASSA_RETURN_URL` | URL возврата после оплаты |

Dev JWT secret и пароли БД захардкожены для локальной разработки — не для production.

## Документация для разработки

- [AGENTS.md](AGENTS.md) — стартовый воркфлоу и Definition of Done
- [PROGRESS.md](PROGRESS.md) — текущее состояние и лог сессий
- [PLANS.md](PLANS.md) — как вести exec plans
- [feature-list.json](feature-list.json) — активные фичи и доказательства верификации
- [ARCHITECTURE.md](ARCHITECTURE.md) — правила слоёв и структуры
