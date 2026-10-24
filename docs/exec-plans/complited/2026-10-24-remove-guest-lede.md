# Exec plan: Убрать guest lede с главной (completed)

## Цель

Убрать с главной страницы надпись:
«Смотрите витрину без входа — войдите через кнопку в шапке, чтобы открыть профиль, избранное и оформление заказа.»

## Скоуп

- `src/main/resources/static/index.html` — удалён `<p class="lede">…</p>`
- `CatalogProfileTests` — assert отсутствия текста на `/`
- Out of scope: редизайн hero; CSS `.lede` оставлен (не мешает)

## Сделано

- Удалён guest lede из `catalog-hero`
- Тест `homePageHasNoTechnicalTokenCopy` проверяет отсутствие «Смотрите витрину без входа»

## Путь верификации

- `mvnw.cmd test` — 23/23 OK
- `docker compose up -d --build` — healthy
- `GET /actuator/health` — UP
- Smoke: `GET /` 200, lede absent, headline present

## Риски

- Корзина по-прежнему только по `X-Cart-Id`
- Порт 8080: конфликт локального run и docker `app`
