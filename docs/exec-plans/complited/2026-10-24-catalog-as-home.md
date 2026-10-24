# Exec plan: каталог как главная страница (completed)

## Цель

При входе на сайт (`/`) показывать каталог товаров. Отдельная страница входа
больше не нужна — вход в профиль выполняется с главной (каталог).

## Скоуп (сделано)

- `index.html` = каталог + боковая панель входа/регистрации
- `/catalog.html` → редирект на `/`
- Удалён `auth.js`; login/register в `catalog.js`
- Тесты обновлены: home = catalog + auth chrome

## Путь верификации (пройден)

1. `docker compose up -d --build` — app healthy `:8080`
2. `mvnw.cmd test` — 22/22 OK
3. `GET /actuator/health` — UP
4. Smoke: `GET /` → product-grid + auth-panel; `/catalog.html` → redirect

## Риски

- Корзина по-прежнему анонимная (`X-Cart-Id`)
- Порт 8080: конфликт local run и docker `app`

## Открытые решения

- Нет блокирующих
