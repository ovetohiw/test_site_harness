# Exec plan: страница товаров и корзина (completed)

## Цель

Страница каталога с товарами (название, картинка, цена, краткое описание),
доступная анонимно; добавление и удаление товаров из корзины.

## Скоуп

- Liquibase: таблица `products` + seed, таблицы `carts` / `cart_items`
- REST: `GET /api/products`, `GET/POST/DELETE /api/cart*` (анонимно по `X-Cart-Id`)
- HTML/CSS/JS: `/catalog.html` + корзина
- SpringBootTest: `CatalogCartTests` (5)

## Out-of-scope

- Оформление заказа / оплата
- Привязка корзины к JWT-пользователю
- Админка товаров

## Путь верификации

1. `docker compose up -d --build` — app+db healthy
2. `mvnw.cmd test` — 15/15 OK
3. `GET /actuator/health` — UP
4. Анонимный products + add/remove cart + `/catalog.html` 200

## Риски и блокеры

- Docker Desktop может зависать при rebuild (перезапуск Desktop)
- Порт 8080: конфликт локального `spring-boot:run` и docker `app`
- Корзина по `X-Cart-Id`, не привязана к JWT

## Лог прогресса

- 2026-10-24: план создан, baseline OK
- 2026-10-24: реализация завершена, верификация пройдена, статус `passing`

## Открытые решения

- Корзина серверная по заголовку `X-Cart-Id` (UUID в localStorage) — принято и реализовано
