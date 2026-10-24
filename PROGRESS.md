# Лог прогресса

## Текущее проверенное состояние

- Корень репозитория: `d:\apps\test_site_harness`
- Стандартный путь старта: `docker compose up -d` затем `mvnw.cmd spring-boot:run` (или `mvn spring-boot:run`)
- Стандартный путь верификации: `mvnw.cmd test` + `GET http://localhost:8080/actuator/health`
- Текущая незавершённая фича с наивысшим приоритетом: нет (task-001 = passing)
- Текущий блокер: нет

## Лог сессий

### Сессия 001

- Дата: 2026-10-24
- Цель: task-001 — PostgreSQL через docker-compose, Liquibase changelog, базовое Spring Boot приложение
- Сделано:
  - Spring Boot 3.3.5 + Maven Wrapper
  - `docker-compose.yml` (PostgreSQL 16)
  - Liquibase master + `001-baseline.yaml`
  - Actuator health + SpringBootTest
  - Временный `002-verification-probe` применён и откатан
- Запущенная верификация:
  - `docker compose up -d` — OK
  - `mvn test` — 3/3 OK (повторно после rollback)
  - `spring-boot:run` + `/actuator/health` — UP
  - `mvn liquibase:rollback -Dliquibase.rollbackCount=1` — OK
- Зафиксированные доказательства: см. `feature-list.json` → task-001.evidence
- Коммиты: `6fb581b` Add Spring Boot baseline with PostgreSQL, Liquibase, and docker-compose (task-001).
- Обновлённые файлы или артефакты: `pom.xml`, `docker-compose.yml`, `src/**`, `mvnw*`, `.mvn/**`, `feature-list.json`, `PROGRESS.md`
- Известный риск или нерешённая проблема:
  - Docker Desktop должен быть запущен до `docker compose up`
  - Локальные credentials БД захардкожены в `application.yml` / compose (только для dev)
  - Системный `mvn` не в PATH; использовать `mvnw.cmd` или кэш wrapper
- Следующий лучший шаг: добавить следующую фичу в `feature-list.json` (домен/каталог/auth) и взять её в работу
