# Exec plan: страница входа / регистрация / JWT (completed)

## Цель

Реализовать регистрацию и вход по логину и паролю с JWT (TTL 1 час).
После обновления страницы пользователь остаётся в системе.

## Скоуп

- Liquibase: таблица `users`
- Spring Security + JWT
- REST: register / login / me
- HTML/CSS/JS: страница входа и регистрации, хранение токена в `localStorage`
- SpringBootTest: happy-path и ошибки auth

## Out-of-scope

- OAuth / social login
- Refresh tokens
- Роли/админка
- Каталог товаров

## Путь верификации

1. `docker compose up -d`
2. `mvnw.cmd test` — 10/10 OK
3. `mvnw.cmd spring-boot:run` + health-check UP
4. register → me; token expiresInMs=3600000; UI `/` отдаёт Aether

## Риски и блокеры

- Битый `distributionUrl` в Maven Wrapper — исправлен на 3.9.9
- Dev JWT secret в `application.yml` (не для prod)

## Лог прогресса

- 2026-10-24: план создан, baseline OK
- 2026-10-24: реализация завершена, верификация пройдена, статус `passing`

## Открытые решения

- Логин = уникальный username (lowercase)
- Persistence сессии на клиенте через `localStorage` + Bearer JWT
- Решение принято и реализовано
