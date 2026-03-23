package utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class JsonLogger {

    private static final Gson gson = new Gson();
    private static final DateTimeFormatter formatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneId.of("UTC"));

    public static void info(String message, JsonObject additionalContext) {
        log("INFO", message, additionalContext, null);
    }

    public static void info(String message) {
        log("INFO", message, null, null);
    }

    public static void error(String message, Throwable throwable) {
        log("ERROR", message, null, throwable);
    }

    public static void error(String message) {
        log("ERROR", message, null, null);
    }

    private static void log(String level, String message, JsonObject context, Throwable throwable) {
        JsonObject logEntry = new JsonObject();
        logEntry.addProperty("timestamp", formatter.format(Instant.now()));
        logEntry.addProperty("level", level);
        logEntry.addProperty("thread", Thread.currentThread().getName());
        logEntry.addProperty("message", message);

        if (context != null) {
            logEntry.add("context", context);
        }

        if (throwable != null) {
            logEntry.addProperty("exception", throwable.getClass().getName());
            logEntry.addProperty("error_message", throwable.getMessage());
        }

        String jsonLog = gson.toJson(logEntry);

        if ("ERROR".equals(level)) {
            System.err.println(jsonLog);
        } else {
            System.out.println(jsonLog);
        }
    }
}
