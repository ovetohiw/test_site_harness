# Exec plan: доработка каталога и профиля (completed)

## Цель

Сортировка каталога (цена / название / бренд), количество на карточке товара,
профиль авторизованного пользователя (аватар, данные, избранное, история покупок,
выход), убрать техническую информацию с пользовательских страниц.

## Скоуп (сделано)

- Liquibase `004-catalog-profile` (brand, avatar_url, favorites, orders/order_items)
- API: `GET /api/products?sort=&dir=`, favorites CRUD, orders list + checkout
- `GET /api/auth/me` → login, avatarUrl, createdAt
- UI: sort toolbar, qty input, profile panel, checkout, cleanup index copy
- `CatalogProfileTests` + обновления baseline tests

## Путь верификации (пройден)

1. `docker compose up -d --build` — app healthy `:8080`
2. `mvnw.cmd test` — 22/22 OK
3. `GET /actuator/health` — UP
4. Smoke: sort by price, favorites, qty→checkout→orders, catalog/profile chrome, index без «Токен»

## Риски

- Корзина по-прежнему анонимная (`X-Cart-Id`), не привязана к JWT
- Checkout доступен только авторизованным
- Docker Desktop / порт 8080

## Открытые решения

- Нет блокирующих
