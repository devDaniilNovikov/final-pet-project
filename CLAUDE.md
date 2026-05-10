# CLAUDE.md

Этот файл предоставляет инструкции для Claude Code (claude.ai/code) при работе с кодом в данном репозитории.

## Команды сборки и запуска

```bash
# Собрать все модули
./gradlew build

# Запустить конкретный сервис
./gradlew :account-service:bootRun
./gradlew :product-service:bootRun
./gradlew :order-service:bootRun
./gradlew :notification-service:bootRun

# Запустить все тесты
./gradlew test

# Запустить тесты конкретного сервиса
./gradlew :order-service:test

# Запустить один тестовый класс
./gradlew :order-service:test --tests "dn.orderservice.OrderServiceIT"
```

## Структура проекта

Многомодульный Gradle-проект (Kotlin DSL). Java 25, Spring Boot 3.5, виртуальные потоки.

```
shared-events/         # Общая библиотека: события, outbox, инфраструктура идемпотентности
account-service/       # Профили пользователей, баны, адреса
product-service/       # Каталог товаров, управление инвентарём
order-service/         # Оркестрация заказов, координатор саги
notification-service/  # Уведомления через Kafka DLT + повторные попытки по расписанию
```

Все сервисы зависят от `shared-events`, но никогда не зависят напрямую друг от друга.

## Архитектура: ключевые паттерны

### Transactional Outbox (`shared-events`)
События записываются в таблицу `marketplace.outbox` в рамках той же транзакции, что и бизнес-сущность. `OutboxRelay` (запускается каждую 1с) опрашивает таблицу с `FOR UPDATE SKIP LOCKED` и отправляет в Kafka без дублирования. Никогда не публиковать в Kafka напрямую — только через outbox.

Ключевые правила:
- `OutboxEntity.id` должен быть установлен в `outboxEvent.eventId()` (идемпотентность на стороне записи — нарушение первичного ключа предотвращает двойную публикацию)
- Все методы `createOutbox` должны быть аннотированы `@Transactional(propagation = MANDATORY)` — они всегда должны выполняться в рамках существующей транзакции
- Бин `ExecutorService` и `ConcurrencyConfig` находятся только в `shared-events`, не в отдельных сервисах

### Идемпотентность (`shared-events`)
`EventProcessor.processEvent(UUID eventId)` вставляет запись в таблицу `processed_events` с `@Transactional(propagation = REQUIRES_NEW)`. Слушатели саги вызывают его первым; при `DataIntegrityViolationException` (дублирующийся ключ) — событие уже обработано, тихо пропустить. `REQUIRES_NEW` критически важен: предотвращает пометку внешней транзакции как rollback-only из-за исключения внутри.

### Хореографическая сага (order ↔ product)
Жизненный цикл заказа управляется через Kafka-события между сервисами:
- `order.created` → ProductSagaListener резервирует инвентарь → публикует `item.reserved` или `item.reserved.failed`
- `item.reserved` → OrderSagaListener подтверждает заказ
- `order.cancelled` → ProductSagaListener восстанавливает инвентарь

Каждый слушатель саги использует `EventProcessor` (идемпотентность через таблицу `processed_events`) для безопасной обработки повторных доставок.

### Синхронный HTTP-вызов
Order-service обращается к product-service по HTTP для пакетных запросов товаров (`ProductClient`). Таймауты подключения и чтения настраиваются через `app.http.client.connect-timeout` / `app.http.client.read-timeout`.

## Переменные окружения

Все сервисы используют следующий паттерн (настраивается в `application.yml` через переменные окружения):

| Переменная | Назначение |
|------------|-----------|
| `SERVER_PORT` | HTTP-порт |
| `POSTGRES_URL` | JDBC URL (у каждого сервиса своя БД) |
| `POSTGRES_USERNAME / PASSWORD` | Учётные данные БД |
| `REDIS_HOST / PORT / REDIS_CACHE_TTL_SECONDS` | Redis-кэш |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka-брокер |
| `LIQUIBASE_CHANGELOG_PATH` | Путь к `db.changelog-master.yaml` |
| `APPLICATION_NAME` | Имя Spring-приложения |
| `HTTP_CLIENT_PRODUCT_SERVICE_URL` | (только order-service) базовый URL product-service |
| `APP_HTTP_CLIENT_CONNECT_TIMEOUT` | (только order-service) таймаут подключения HTTP в мс |
| `APP_HTTP_CLIENT_READ_TIMEOUT` | (только order-service) таймаут чтения HTTP в мс |

## Соглашения по базе данных

- Схема: `marketplace` (все таблицы, включая outbox и processed_events)
- Миграции: Liquibase; `hibernate.ddl-auto: validate` — схема должна существовать до запуска
- Пессимистичная блокировка применяется там, где необходимо: `@Lock(PESSIMISTIC_WRITE)` в JPA-запросах или нативный `FOR UPDATE SKIP LOCKED`

## Kafka-топики

| Топик | Производитель | Потребитель |
|-------|---------------|-------------|
| `order.created` | order-service | product-service, notification-service |
| `order.confirmed` / `order.paid` / `order.cancelled` | order-service | — |
| `item.reserved` / `item.reserved.failed` | product-service | order-service |
| `payment.success` / `payment.failed` | (будущее) | order-service |
| `account.created` / `account.banned` / и др. | account-service | notification-service |

Для каждой consumer-группы настроены Dead Letter Topics (DLT).

## Тестирование

- Юнит-тесты: чистый JUnit 5 + AssertJ + Mockito
- Интеграционные тесты: TestContainers (реальный PostgreSQL), именуются с суффиксом `IT`
- Асинхронные проверки: использовать `Awaitility` (уже есть в зависимостях order-service)
- Паттерн базового класса: `AbstractProductIT` настраивает общий контекст TestContainers

## CodeGraph

`.codegraph/` существует — использовать `codegraph_explore` (через агент Explore) для вопросов по кодовой базе. Использовать `codegraph_search` / `codegraph_callers` / `codegraph_impact` напрямую в основной сессии для точечного поиска символов перед правками.
