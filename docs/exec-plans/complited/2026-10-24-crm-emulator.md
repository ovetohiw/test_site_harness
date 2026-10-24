# Exec plan: CRM emulator (completed)

## Цель

Создать Java/Spring Boot эмулятор CRM (Bitrix24 + amoCRM) для локального
тестирования интеграции магазина, с опциональным развёртыванием в Docker.

## Скоуп (сделано)

- Отдельное Spring Boot приложение `crm-emulator/` (порт 8090)
- Bitrix24 webhook: `crm.contact.add`, `crm.deal.add`, `crm.timeline.comment.add`
- amoCRM API v4: `/api/v4/contacts`, `/api/v4/leads`, `/api/v4/contacts/notes`
- Inspect API: `GET/DELETE /emulator/events`, `GET /emulator/status`
- Actuator health
- Опциональный Docker: `crm-emulator/Dockerfile` + compose profile `with-crm-emulator`

## Путь верификации (пройден)

1. `.\mvnw.cmd test` — shop OK (28)
2. `.\mvnw.cmd -f crm-emulator/pom.xml test` — 6/6 OK
3. `docker compose --profile with-crm-emulator up -d --build crm-emulator` — healthy `:8090`
4. `GET /actuator/health` (shop `:8080` + emulator `:8090`) — UP
5. E2E: shop live → emulator → register/favorite/checkout → 6 SUCCESS + 6 emulator events

## Запуск

```bash
# только эмулятор
docker compose --profile with-crm-emulator up -d --build crm-emulator

# магазин против эмулятора (compose network)
BITRIX24_WEBHOOK_BASE_URL=http://crm-emulator:8090/rest/1/test/ \
AMOCRM_BASE_URL=http://crm-emulator:8090 \
AMOCRM_ACCESS_TOKEN=test-token \
docker compose up -d app
```

Локально без Docker: `.\mvnw.cmd -f crm-emulator/pom.xml spring-boot:run`

## Риски

- Порт 8090 должен быть свободен
- Без profile `with-crm-emulator` сервис не стартует (это ожидаемо)
- Эмулятор in-memory: события теряются при рестарте

## Открытые решения

- Нет блокирующих
