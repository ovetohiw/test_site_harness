# Exec plan: ЮKassa payments + emulator (completed)

## Цель

Интегрировать оплату заказов через ЮKassa и добавить локальный эмулятор
платёжного API для тестов (по аналогии с CRM emulator).

## Скоуп (сделано)

- Клиент ЮKassa API v3 (`create`/`get`) + stub без `shopId`/`secretKey`
- Liquibase `006-payments.yaml`: `orders.payment_status`, таблица `payments`
- Checkout создаёт платёж; CRM `SALE_COMPLETED` — после успешной оплаты
- API: `GET /api/payments/status`, `GET /api/payments`, `POST /api/payments/{id}/refresh`,
  `POST /api/payments/webhook`
- UI: статус оплаты и ссылка «Оплатить» в истории заказов
- `payment-emulator/` (порт 8091) + Docker profile `with-payment-emulator`

## Путь верификации (пройден)

1. `.\mvnw.cmd test` — shop 34/34 OK
2. `.\mvnw.cmd -f payment-emulator/pom.xml test` — 4/4 OK
3. `docker compose --profile with-payment-emulator up -d --build` — payment-emulator healthy `:8091`
4. `GET /actuator/health` shop+emulator — UP
5. E2E live: checkout PENDING → emulator succeed → refresh SUCCEEDED (events=3)

## Запуск

```bash
# эмулятор
docker compose --profile with-payment-emulator up -d --build payment-emulator

# магазин против эмулятора
YOOKASSA_BASE_URL=http://payment-emulator:8091/v3 \
YOOKASSA_SHOP_ID=test-shop \
YOOKASSA_SECRET_KEY=test-secret \
docker compose up -d app
```

Локально без Docker: `.\mvnw.cmd -f payment-emulator/pom.xml spring-boot:run`

## Риски

- Порт 8091 должен быть свободен
- Без profile `with-payment-emulator` контейнер не стартует (опционально)
- Без `YOOKASSA_SHOP_ID`/`SECRET_KEY` — stub (мгновенный SUCCEEDED)
- Webhook без IP whitelist (только для dev)

## Открытые решения

- Нет блокирующих
