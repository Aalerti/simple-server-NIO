# 🖥️ SimpleServer — Java NIO HTTP Server

> Самописный HTTP-сервер на чистой Java без каких-либо фреймворков.  
> Реализован с использованием **Java NIO (Non-blocking I/O)**, **H2 Database** и **REST API** архитектуры.

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

---

## ⚙️ Технологии

| Технология | Назначение |
|---|---|
| **Java 21** | Основной язык |
| **Java NIO** | Non-blocking I/O, `Selector`, `SocketChannel` |
| **CompletableFuture** | Асинхронная обработка запросов |
| **H2 Database** | Встроенная реляционная база данных |
| **Gson** | Сериализация/десериализация JSON |
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
├── pom.xml
└── README.md
```

---

## 🚀 Быстрый старт

### Требования

- **Java 21+**
- **Maven 3.8+**

### Запуск

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

## 📡 API Reference

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
