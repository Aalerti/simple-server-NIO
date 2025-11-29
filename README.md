# High-Performance Java NIO Web Server

This is a lightweight, non-blocking HTTP web server built from scratch using Java NIO (New I/O) libraries. It implements the Reactor pattern to handle thousands of concurrent connections efficiently.

## Features

* **Non-blocking Architecture:** Uses `Selector` and `SocketChannel` for high concurrency.
* **Event Loop & Worker Pool:** Separates I/O processing from business logic to prevent blocking.
* **Custom HTTP Parsing:** Manual parsing of HTTP methods, headers, and body (supports byte-level data).
* **Routing System:** Flexible `Router` to map URLs to specific `Handlers`.
* **Static File Serving:** Serves HTML, CSS, JS, and images from the `static/` directory.
* **REST API Support:** Handles JSON POST requests using Gson.

## Tech Stack

* **Language:** Java 17+
* **Core:** `java.nio.*` (No Tomcat, Jetty, or Netty used)
* **Concurrency:** `CompletableFuture`, `ExecutorService`
* **JSON Processing:** Google Gson

## Architecture

1.  **Main Loop:** A single thread accepts connections and reads data into buffers.
2.  **Request Assembly:** Data is accumulated until the full HTTP header is received.
3.  **Async Processing:** Complete requests are offloaded to a thread pool.
4.  **Routing:** The `Router` dispatches the request to the appropriate implementation of the `Handler` interface.
5.  **Response:** Bytes are written back to the non-blocking channel.

## Performance

Tested with Apache Bench (`ab`) on a local machine:
* **RPS (Requests Per Second):** ~6400+
* **Concurrency:** 100 simultaneous connections
* **Failed Requests:** 0

## How to Run

1.  Clone the repository.
2.  Build with Maven: `mvn clean package`.
3.  Run the Main class: `Main`.
4.  Open browser: `http://localhost:8080`.

---
*Created for educational purposes to understand the low-level mechanics of HTTP servers.*