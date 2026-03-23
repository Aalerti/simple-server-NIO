# 🖥️ SimpleServer — Java NIO HTTP Server (SRE-Ready)

> Самописный HTTP-сервер на чистой Java без каких-либо фреймворков.  
> Реализован с использованием **Java NIO (Non-blocking I/O)**, **H2 Database**, **REST API** архитектуры и современных **SRE-практик** (Observability & Reliability).

---

## 📌 О проекте

**SimpleServer** — это учебный проект, демонстрирующий глубокое понимание того, как работают HTTP-серверы изнутри. Вместо того чтобы спрятаться за Spring или Tomcat, здесь вручную реализованы:

- Приём TCP-соединений через `ServerSocketChannel`
- Неблокирующая обработка запросов через `Selector`
- Парсинг сырых HTTP-запросов из байтов
- Маршрутизация (роутер) запросов по URL-путям
- REST API для управления пользователями (CRUD)
- Раздача статических файлов (HTML, CSS, JS, изображения)
- Персистентное хранение данных в H2 Database
- **Reliability:** Корректное завершение работы (Graceful Shutdown) и Liveness-пробы (`/health`)
- **Observability:** Структурированное JSON-логирование и сбор RED-метрик (Micrometer + Prometheus)

---

## ⚙️ Технологии

| Технология | Назначение |
|---|---|
| **Java 21** | Основной язык |
| **Java NIO** | Non-blocking I/O, `Selector`, `SocketChannel` |
| **CompletableFuture** | Асинхронная обработка запросов |
| **H2 Database** | Встроенная реляционная база данных |
| **Gson** | Сериализация/десериализация JSON |
| **Micrometer** | Фасад для сбора application-метрик (Timer, Counter) |
| **Prometheus & Grafana** | Time-series СУБД для метрик и визуализация дашбордов |
| **Docker & Compose** | Изоляция среды и запуск инфраструктуры |
| **Maven** | Управление зависимостями и сборкой |

---

## 🗂️ Структура проекта

```
SimpleServer/
├── src/main/java/
│   ├── Main.java                  # Точка входа. NIO Event Loop
│   ├── database/
│   │   ├── Database.java          # Подключение и инициализация H2
│   │   └── UserRepository.java    # CRUD-операции с таблицей users
│   ├── http/
│   │   ├── Handler.java           # Функциональный интерфейс обработчика
│   │   ├── HttpMethods.java        # Enum: GET, POST, PUT, DELETE
│   │   ├── HttpRequest.java        # Парсинг сырого HTTP-запроса из байтов
│   │   ├── HttpResponse.java       # Построение HTTP-ответа
│   │   ├── Router.java             # Маршрутизация запросов по путям
│   │   ├── UsersHandler.java       # Обработчик /api/users/ (REST API)
│   │   ├── LoginHandler.java       # Обработчик /login/
│   │   ├── MainPageHandler.java    # Обработчик главной страницы
│   │   └── StaticFileHandler.java  # Раздача статических файлов
│   ├── model/
│   │   └── User.java               # Модель пользователя с валидацией
│   └── utils/
│       └── Validators.java         # Валидация username, email, password
├── static/
│   └── index.html                  # Главная страница (отдаётся сервером)
├── docker-compose.yml              # Оркестрация сервера, Prometheus, Grafana
├── prometheus.yml                  # Конфигурация сбора метрик
├── pom.xml
└── README.md
```

---

## 🏗️ Архитектура системы

Ниже представлена полная диаграмма потока запроса — от клиента до базы данных и мониторинга:

```
┌─────────────────────────────────────────────────────────────────────┐
│                          CLIENT (HTTP)                              │
└─────────────────────────┬───────────────────────────────────────────┘
                          │ TCP-соединение (порт 8080)
                          ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     NIO EVENT LOOP (Main Thread)                    │
│                                                                     │
│   ┌──────────────┐      OP_ACCEPT     ┌────────────────────────┐   │
│   │ ServerSocket │ ─────────────────► │       Selector         │   │
│   │   Channel    │                    │                        │   │
│   └──────────────┘      OP_READ       │  следит за ВСЕМИ       │   │
│                         ◄─────────── │  соединениями сразу    │   │
│                                       └───────────┬────────────┘   │
└───────────────────────────────────────────────────┼────────────────┘
                                                    │ данные готовы
                                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   THREAD POOL (ExecutorService)                     │
│                                                                     │
│  CompletableFuture.supplyAsync()  →  thenApply()  →  thenAccept()  │
│         │                                │                │        │
│    [parse bytes]                   [route request]  [write response]│
│    HttpRequest                        Router            SocketChannel│
└──────────────────────────┬──────────────────────────────────────────┘
                           │
              ┌────────────┴──────────────┐
              ▼                           ▼
┌─────────────────────┐     ┌──────────────────────────────────┐
│      HANDLERS       │     │         OBSERVABILITY            │
│                     │     │                                  │
│ /api/users/         │     │  GET /health  →  {"status":"UP"} │
│   UsersHandler      │     │  GET /metrics →  Prometheus fmt  │
│                     │     │                                  │
│ /login/             │     │  Structured JSON Logging         │
│   LoginHandler      │     │  (ELK-ready, ISO 8601 UTC)       │
│                     │     └──────────────┬───────────────────┘
│ /static/*           │                    │ scrape
│   StaticFileHandler │                    ▼
└────────┬────────────┘     ┌──────────────────────────────────┐
         │ JDBC             │         PROMETHEUS               │
         ▼                  │   (time-series метрики)          │
┌─────────────────────┐     └──────────────┬───────────────────┘
│    H2 DATABASE      │                    │
│                     │                    ▼
│  TABLE: users       │     ┌──────────────────────────────────┐
│  - id (PK)          │     │           GRAFANA                │
│  - username         │     │   Дашборды: RPS, Latency, Errors │
│  - email            │     └──────────────────────────────────┘
│  - password         │
└─────────────────────┘
```

### Жизненный цикл одного запроса

```
1. CLIENT          → TCP SYN → ServerSocketChannel
2. Selector        → OP_ACCEPT → регистрирует SocketChannel + ByteArrayOutputStream
3. Selector        → OP_READ  → читает байты в ByteBuffer (1024 byte chunks)
4. isRequestComplete() → проверяет наличие \r\n\r\n и Content-Length
5. ExecutorService → CompletableFuture.supplyAsync(new HttpRequest(bytes))
6. HttpRequest     → парсит метод, путь, заголовки, тело
7. Router          → ищет Handler по точному совпадению → затем по частичному
8. Handler         → бизнес-логика → UserRepository → H2 Database
9. HttpResponse    → сериализует статус + заголовки + тело в байты
10. SocketChannel  → write(ByteBuffer) → close()
```

---

## 🚀 Быстрый старт

### Вариант 1: Docker Compose (рекомендуется)

Запускает сервер вместе с Prometheus и Grafana одной командой. Никаких предустановленных зависимостей, кроме Docker.

#### Требования

- **Docker** и **Docker Compose**

#### Запуск

```bash
# 1. Клонировать репозиторий
git clone https://github.com/your-username/SimpleServer.git
cd SimpleServer

# 2. Поднять весь стек одной командой
docker compose up --build
```

#### Доступные сервисы

| Сервис | Адрес | Описание |
|---|---|---|
| 🌐 **HTTP-сервер** | http://localhost:8080 | Основное приложение |
| 📊 **Grafana** | http://localhost:3000 | Дашборды (admin / admin) |
| 📈 **Prometheus** | http://localhost:9090 | Raw-метрики |

#### Остановка

```bash
# Остановить все контейнеры
docker compose down

# Остановить и удалить volumes (данные метрик)
docker compose down -v
```

---

### Вариант 2: Локальный запуск (без Docker)

#### Требования

- **Java 21+**
- **Maven 3.8+**

#### Запуск

```bash
# 1. Клонировать репозиторий
git clone https://github.com/your-username/SimpleServer.git
cd SimpleServer

# 2. Собрать проект
mvn compile

# 3. Запустить сервер
mvn exec:java -Dexec.mainClass="Main"
```

Сервер запустится на **http://localhost:8080**

---

## 🛡️ SRE & Production-Readiness

В проекте применены инженерные практики для обеспечения надёжности и наблюдаемости системы.

### Graceful Shutdown

При получении сигнала `SIGTERM` (например, от Kubernetes при деплое или `docker stop`) сервер **не обрывает соединения резко**, а завершает работу корректно:

```
SIGTERM получен
    │
    ├─► ServerSocketChannel.close()    # перестаём принимать НОВЫЕ соединения
    │
    ├─► активные SocketChannel-ы       # корректно закрываем текущие соединения
    │
    └─► ExecutorService.shutdown()     # ждём завершения текущих задач (5 сек)
            │
            └─► awaitTermination(5, SECONDS)
                    │
                    ├─ [успех] → все транзакции БД завершены, выход 0
                    └─ [таймаут] → shutdownNow() → принудительное завершение
```

Это критично в production: без Graceful Shutdown запрос, который уже читает из БД, может получить `ConnectionResetException` на стороне клиента.

---

### Structured JSON Logging

Вместо обычного текста логи пишутся в машиночитаемом формате JSON — это делает их совместимыми с ELK-стеком (Elasticsearch + Logstash + Kibana):

```json
{
  "timestamp": "2025-01-15T14:32:01.123Z",
  "level": "INFO",
  "thread": "pool-1-thread-3",
  "message": "Request handled",
  "context": {
    "method": "GET",
    "path": "/api/users/",
    "status": 200,
    "duration_ms": 4
  }
}
```

Каждое событие содержит UTC-таймстемп в формате ISO 8601, имя потока (для диагностики race condition-ов) и контекст запроса.

---

### RED-метрики (Prometheus + Micrometer)

Сбор метрик реализован по методологии **RED** — стандарту SRE-инженеров:

| Буква | Метрика | Описание |
|---|---|---|
| **R** — Rate | `http_requests_total` | Запросов в секунду (RPS) |
| **E** — Errors | `http_errors_total` | Количество ответов 4xx / 5xx |
| **D** — Duration | `http_request_duration_seconds` | Время обработки запроса (гистограмма) |

Защита от **Cardinality Explosion**: вместо записи полного URL в метку (что привело бы к миллионам уникальных рядов данных) используется только базовый путь: `/api/users/` вместо `/api/users/123`.

Пример raw-метрик на эндпоинте `/metrics`:

```
# HELP http_requests_total Total number of HTTP requests
# TYPE http_requests_total counter
http_requests_total{method="GET",path="/api/users/",status="200"} 1547.0

# HELP http_request_duration_seconds Request duration
# TYPE http_request_duration_seconds histogram
http_request_duration_seconds_bucket{le="0.005"} 1389.0
http_request_duration_seconds_bucket{le="0.01"}  1521.0
http_request_duration_seconds_bucket{le="+Inf"}  1547.0
```

---

### Health Checks

Эндпоинт `/health` используется балансировщиками нагрузки (Nginx, Kubernetes Liveness Probe) для проверки состояния сервера:

```http
GET /health HTTP/1.1
Host: localhost:8080
```

```json
{ "status": "UP" }
```

Если сервер не отвечает на `/health` — балансировщик исключает его из пула и трафик уходит на здоровые инстансы.

---

## 📡 API Reference

### Системные эндпоинты

| Эндпоинт | Метод | Описание |
|---|---|---|
| `/health` | GET | Liveness-проба: `{"status": "UP"}` |
| `/metrics` | GET | Prometheus-метрики (RPS, Latency, Errors) |

---

### Пользователи `/api/users/`

#### Получить всех пользователей

```http
GET /api/users/
```

**Ответ `200 OK`:**
```json
[
  {
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com",
    "password": "securepass"
  }
]
```

---

#### Получить пользователя по ID

```http
GET /api/users/{id}
```

**Ответ `200 OK`:**
```json
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "password": "securepass"
}
```

**Ответ `404 Not Found`:**
```json
{ "error": "User not found" }
```

---

#### Создать пользователя

```http
POST /api/users/
Content-Type: application/json
```

**Тело запроса:**
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "securepass"
}
```

**Ответ `201 Created`:**
```json
{
  "status": "created",
  "user": { ... }
}
```

---

#### Обновить пользователя

```http
PUT /api/users/{id}
Content-Type: application/json
```

**Тело запроса:**
```json
{
  "username": "new_name",
  "email": "new@example.com",
  "password": "newpassword123"
}
```

**Ответ `200 OK`:**
```json
{
  "status": "updated",
  "user": { ... }
}
```

---

#### Удалить пользователя

```http
DELETE /api/users/{id}
```

**Ответ `200 OK`:**
```json
{
  "status": "delete",
  "user": { ... }
}
```

---

## 🏗️ Архитектурные решения

### Non-blocking I/O (NIO) и Event Loop

Сервер не создаёт отдельный поток на каждое соединение (что не масштабируется). Вместо этого используется `Selector` — один поток следит за несколькими соединениями и реагирует только тогда, когда данные готовы.

```
[Selector] → OP_ACCEPT → принять соединение → зарегистрировать в Selector
           → OP_READ   → прочитать байты   → передать в ThreadPool
```

### Асинхронная обработка через CompletableFuture

После сборки полного запроса его обработка передаётся в пул потоков (`ExecutorService`), не блокируя Event Loop:

```
handleRead() → processRequest()
    → CompletableFuture.supplyAsync(parse) → thenApply(route) → thenAccept(write)
```

### Роутер (Router)

Регистрация маршрутов происходит в `Main.java`. Роутер сначала ищет точное совпадение пути, затем — частичное:

```java
router.register("/api/users/", new UsersHandler());
router.register("/login/",     new LoginHandler());
router.register("/",           new StaticFileHandler());
```

### Валидация данных

Все входящие данные пользователя проверяются через `Validators` до записи в базу:

- `username` не может быть пустым
- `email` обязан содержать символ `@`
- `password` должен быть не короче 8 символов

---

## 🛡️ Коды ответов

| Код | Значение |
|---|---|
| `200 OK` | Успешный запрос |
| `201 Created` | Ресурс успешно создан |
| `400 Bad Request` | Некорректный запрос или тело |
| `404 Not Found` | Ресурс не найден |
| `405 Method Not Allowed` | HTTP-метод не поддерживается |
| `415 Unsupported Media Type` | Ожидается `application/json` |
| `500 Internal Server Error` | Внутренняя ошибка сервера |

---

## 📦 Зависимости (pom.xml)

```xml
<!-- Google Gson — JSON сериализация -->
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>

<!-- H2 Database — встроенная SQL БД -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <version>2.2.224</version>
</dependency>

<!-- Micrometer — фасад для метрик -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <version>1.12.0</version>
</dependency>
```

---

## 💡 Чему учит этот проект

- Как работают HTTP-серверы на уровне сокетов и байтов
- Разница между blocking I/O и non-blocking I/O
- Паттерн `Reactor` (Event Loop + Selector)
- Ручной парсинг HTTP: метод, путь, заголовки, тело
- Построение REST API без фреймворков
- Работа с JDBC и подготовленными запросами (`PreparedStatement`)
- Разделение ответственности: Router, Handler, Repository, Model
- SRE-практики: Graceful Shutdown, Health Checks, RED-метрики
- Контейнеризация приложения и инфраструктуры мониторинга

---

## ⚡ Производительность

Нагрузочное тестирование проводилось с помощью **Apache Bench (ab)** — стандартного инструмента для измерения пропускной способности HTTP-серверов.

### Условия теста

```bash
ab -n 10000 -c 100 http://localhost:8080/api/users/
```

- `-n 10000` — общее количество запросов
- `-c 100` — количество одновременных (параллельных) соединений
- Тестировался эндпоинт `GET /api/users/` с реальным обращением к H2 Database

### Результаты

| Метрика | Значение |
|---|---|
| **Запросов в секунду (RPS)** | ~6 400 req/sec |
| **Параллельных соединений** | 100 |
| **Ошибок** | 0 |
| **Failed requests** | 0 |

### Почему такая производительность?

Результат достигается благодаря архитектуре **Reactor Pattern**:

**Классический подход (Thread-per-Request)** — на каждый входящий запрос создаётся отдельный поток. При 100 одновременных соединениях в системе живут 100 потоков. Каждый поток потребляет память (~1 МБ стека), и операционная система тратит время на переключение между ними (context switching). При высокой нагрузке это становится узким местом.

**NIO + Selector (подход этого проекта)** — один поток `Selector` следит за всеми соединениями и уведомляет только тогда, когда данные реально готовы. Тяжёлая обработка (парсинг, база данных) уходит в `ExecutorService` с кешируемым пулом потоков. Event Loop при этом не блокируется и сразу готов принимать новые соединения.

```
Без NIO:   [conn1 → thread1] [conn2 → thread2] ... [conn100 → thread100]  ← 100 потоков

С NIO:     [Selector] → видит активные соединения → передаёт в ThreadPool
                      ↑ один поток, не блокируется никогда
```

Именно поэтому NIO-сервер при тех же аппаратных ресурсах обрабатывает больше запросов с меньшими задержками.

---

*Создано с нуля на чистой Java — без Spring, без Tomcat, без магии.*
