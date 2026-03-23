import com.google.gson.JsonObject;
import database.Database;
import http.*;
import utils.JsonLogger;
import utils.Metrics;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final Router router = new Router();
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    private static volatile boolean isRunning = true;
    private static Selector selector;
    private static ServerSocketChannel serverChannel;

    public static void main(String[] args) throws IOException {

        Database.initialize();

        serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(8080));
        serverChannel.configureBlocking(false);

        selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        initRouters();
        setupShutdownHook();

        JsonLogger.info("Server started on port 8080");

        while (isRunning) {
            try {
                selector.select();

                if (!isRunning) {
                    break;
                }

                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (!key.isValid()) continue;

                    if (key.isAcceptable()) {
                        handleAccept(serverChannel, selector);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    }
                }
            } catch (IOException e) {
                if (isRunning) {
                    JsonLogger.error("Error in Event Loop", e);
                }
            }
        }
    }

    private static void initRouters() {
        router.register("/api/users/", new UsersHandler());
        router.register("/login/", new LoginHandler());
        router.register("/health", new HealthHandler());
        router.register("/metrics", new MetricsHandler());
        router.register("/", new StaticFileHandler());
    }

    private static void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            JsonLogger.info("Received SIGTERM. Initiating Graceful Shutdown...");
            isRunning = false;

            try {
                if (selector != null) {
                    selector.wakeup();
                }

                if (serverChannel != null) {
                    serverChannel.close();
                }

                if (selector != null) {
                    for (SelectionKey key : selector.keys()) {
                        try {
                            key.channel().close();
                        } catch (IOException e) {
                        }
                    }
                    selector.close();
                    JsonLogger.info("Selector and ServerSocketChannel closed.");
                }

                executor.shutdown();
                JsonLogger.info("Awaiting Shutdown...");
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    JsonLogger.error("Thread pool termination timeout. Forcing shutdown.");
                    executor.shutdownNow();
                } else {
                    JsonLogger.info("All active tasks completed successfully.");
                }

            } catch (Exception e) {
                JsonLogger.error("Error during Graceful Shutdown", e);
            }

            JsonLogger.info("Server completely stopped.");
        }));
    }

    private static void handleAccept(ServerSocketChannel serverChannel, Selector selector) throws IOException {
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        SelectionKey selectionKey = clientChannel.register(selector, SelectionKey.OP_READ);
        selectionKey.attach(new ByteArrayOutputStream());
    }

    private static void handleRead(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
        int bytesRead = clientChannel.read(readBuffer);

        if (bytesRead == -1) {
            clientChannel.close();
            return;
        }

        ByteArrayOutputStream bytesOfRequest = (ByteArrayOutputStream) key.attachment();
        while (bytesRead > 0) {
            readBuffer.flip();
            bytesOfRequest.write(readBuffer.array(), 0, bytesRead);

            readBuffer.clear();
            bytesRead = clientChannel.read(readBuffer);
        }

        byte[] copyBytesOfRequest = bytesOfRequest.toByteArray();

        if (!isRequestComplete(copyBytesOfRequest)) return;

        bytesOfRequest.reset();

        processRequest(copyBytesOfRequest, clientChannel);
    }

    private static boolean isRequestComplete(byte[] bytes) {
        int splitIndex = findHeaderEnd(bytes);
        if (splitIndex == -1) return false;

        int contentLength = extractContentLength(bytes, splitIndex);

        return bytes.length >= (splitIndex + 4 + contentLength);
    }

    private static int findHeaderEnd(byte[] bytes) {
        for (int i = 0; i <= bytes.length - 4; i++) {
            if (bytes[i] == 13 && bytes[i + 1] == 10 &&
                    bytes[i + 2] == 13 && bytes[i + 3] == 10) {
                return i;
            }
        }
        return -1;
    }

    private static int extractContentLength(byte[] bytes, int splitIndex) {
        String headers = new String(bytes, 0, splitIndex, StandardCharsets.UTF_8);
        String[] lines = headers.split("\r\n");

        for (String line : lines) {
            if (line.toLowerCase().startsWith("content-length:")) {
                try {
                    return Integer.parseInt(line.split(":")[1].trim());
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0; // For GET request
    }

    private static void processRequest(byte[] copyBytesOfRequest, SocketChannel clientChannel) {
        long startTime = System.nanoTime();

        CompletableFuture<Void> futureResponse = CompletableFuture.supplyAsync(() -> {
                    HttpRequest req = new HttpRequest(copyBytesOfRequest);

                    JsonObject context = new JsonObject();
                    context.addProperty("method", req.getMethod().toString());
                    context.addProperty("path", req.getPath());
                    JsonLogger.info("Incoming HTTP request", context);

                    return req;
                }, executor)
                .thenApply(req -> {
                    HttpResponse response = router.handle(req);
                    String statusCode = response.getStatus().split(" ")[0];
                    Metrics.registry
                            .timer("http_server_requests_seconds",
                                    "method", req.getMethod().name(),
                                    "status", statusCode)
                            .record(System.nanoTime() - startTime, java.util.concurrent.TimeUnit.NANOSECONDS);

                    return response;

                })
                .exceptionally(ex -> {
                    Metrics.registry.timer("http_server_requests_seconds",
                                    "method", "UNKNOWN",
                                    "status", "500")
                            .record(System.nanoTime() - startTime, java.util.concurrent.TimeUnit.NANOSECONDS);


                    JsonLogger.error("Request processing failed", ex);
                    return new HttpResponse("500 Internal Server Error", "text/plain", "Internal Error");
                })
                .thenAccept(response -> {
                    byte[] responseBytes = response.getBytes();

                    if (responseBytes != null) {
                        ByteBuffer writeBuffer = ByteBuffer.wrap(responseBytes);
                        try {
                            clientChannel.write(writeBuffer);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        clientChannel.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }
}