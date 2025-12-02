# Custom Java NIO Web Server

A high-performance, asynchronous HTTP server built from scratch in Java, without using web containers (Tomcat/Jetty) or frameworks (Spring). The project implements the **Reactor** architectural pattern and a full-featured **REST API** with database integration.

## Features

* **Non-blocking I/O:** Built on `java.nio.channels` (Selector, SocketChannel). A single thread handles thousands of connections.
* **Reactor Pattern:** Separation of network I/O (Event Loop) and business logic (Worker Thread Pool).
* **Database Integration:** Embedded **H2** database. Uses raw **JDBC** (no Hibernate) for maximum control and performance.
* **Full CRUD:** Complete support for Create, Read, Update, Delete operations for User entities.
* **REST API:** JSON request and response processing (using Google Gson).
* **Packet Reassembly:** Correct reassembly of fragmented TCP packets to handle large requests.
* **Static Files:** Serves static content (HTML, CSS, JS, images) from disk.

## Tech Stack

* **Java 17+**
* **Core:** `java.nio`, `java.util.concurrent` (CompletableFuture)
* **Database:** H2 Database Engine, JDBC API
* **JSON:** Google Gson
* **Build:** Maven

## Architecture

The project is layered for code cleanliness:

* **`http`**: Web server logic.
    * `Router`: Maps requests to the appropriate handler.
    * `HttpRequest` / `HttpResponse`: Parses raw bytes and constructs responses.
    * `Handler`: Interface for processing strategies (ApiHandler, StaticFileHandler).
* **`database`**: Data access layer.
    * `Database`: Connection management and table initialization.
    * `UserRepository`: SQL implementation using `PreparedStatement`.
* **`model`**: Entities (POJO) and DTOs.
* **`utils`**: Data validators.

## API Endpoints

The server provides an API for user management at `/api/users/`.

| Method | URL | Description | Request Body (JSON) |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/users/` | Get list of all users | - |
| **GET** | `/api/users/{id}` | Get user by ID | - |
| **POST** | `/api/users/` | Create a new user | `{"username": "Max", "email": "m@test.com", "password": "123"}` |
| **PUT** | `/api/users/{id}` | Update user data | `{"username": "NewName", "email": "new@test.com", "password": "123"}` |
| **DELETE** | `/api/users/{id}` | Delete user | - |

## Run & Test

1.  **Build:**
    ```bash
    mvn clean package
    ```

2.  **Run:**
    Execute the `Main` class. The server will automatically create the `testdb.mv.db` database file and the `users` table.
    Default port: **8080**.

3.  **Testing:**
    * Open in browser: `http://localhost:8080/` (Static pages)
    * Check API via Postman or curl:
        ```bash
        curl -X POST http://localhost:8080/api/users/ \
        -H "Content-Type: application/json" \
        -d '{"username":"User1", "email":"u@u.com", "password":"password123"}'
        ```

## Performance

Apache Bench testing showed high throughput due to the NIO architecture:
* **RPS:** ~6400 req/sec
* **Concurrency:** 100 simultaneous connections
* **Errors:** 0

---
*Created for educational purposes to understand the low-level mechanics of HTTP servers and databases.*