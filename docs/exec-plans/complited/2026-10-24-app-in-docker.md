# Exec plan: приложение в Docker (completed)

## Цель

Приложение Spring Boot разворачивается в контейнере Docker вместе с PostgreSQL
через `docker compose up -d` и доступно на порте 8080 с проходящим health-check.

## Скоуп

- `Dockerfile` (multi-stage: Maven build + JRE runtime)
- Сервис `app` в `docker-compose.yml` (порт 8080, зависимость от healthy `db`)
- Переопределение datasource через env (`jdbc:postgresql://db:5432/...`)
- Верификация: compose up + `/actuator/health` + `mvnw test`

## Out-of-scope

- Prod-ready secrets / secrets manager
- Kubernetes / cloud deploy
- CI image registry publish
- Каталог товаров

## Путь верификации

1. `docker compose up -d --build`
2. Дождаться healthy у `perfume-shop-app`
3. `GET http://localhost:8080/actuator/health` → `{"status":"UP"}`
4. `mvnw.cmd test` — все тесты OK

## Риски и блокеры

- Первый build образа долгий (Maven deps в Docker layer)
- Порт 8080 должен быть свободен на хосте
- Локальный `spring-boot:run` и docker-app не должны слушать 8080 одновременно

## Лог прогресса

- 2026-10-24: план создан, статус `in_progress`
- 2026-10-24: Dockerfile + compose `app` добавлены; верификация пройдена; статус `passing`

## Открытые решения

- DB host в контейнере: сервисное имя `db` (Docker network)
- Healthcheck приложения: curl + `/actuator/health`
- Решение принято и реализовано
