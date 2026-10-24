# Exec plan: CRM Bitrix24 + amoCRM (completed)

## Цель

Подключить CRM (Битрикс24 и amoCRM) для отслеживания продаж и анализа
покупательского поведения.

## Скоуп (сделано)

- `CrmClient` + `Bitrix24CrmClient` (webhook: contact/deal/timeline) + `AmoCrmClient` (API v4: contacts/leads/notes)
- Хуки: register → контакт; checkout → сделка/лид; favorite → поведение
- Liquibase `005-crm-sync` → таблица `crm_sync_events`
- API: `GET /api/crm/status`, `GET /api/crm/events` (JWT)
- Stub-режим без credentials (локальный SUCCESS); live через env:
  `BITRIX24_WEBHOOK_BASE_URL`, `AMOCRM_BASE_URL`, `AMOCRM_ACCESS_TOKEN`
- Тесты: `CrmIntegrationTests` (3) + клиентские unit (2)

## Путь верификации (пройден)

1. `docker compose up -d --build` — app healthy `:8080`
2. `mvnw.cmd test` — 28/28 OK
3. `GET /actuator/health` — UP
4. Smoke: register/favorite/checkout → 6 SUCCESS событий (BITRIX24 + AMOCRM)

## Риски

- Без реальных webhook/token внешние CRM не вызываются (stub)
- Сбой внешнего CRM логируется как FAILED, checkout/register не падают
- Корзина по-прежнему по `X-Cart-Id`, не привязана к JWT

## Открытые решения

- Нет блокирующих
