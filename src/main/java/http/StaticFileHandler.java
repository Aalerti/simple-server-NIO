package http;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StaticFileHandler implements Handler {

    private static final String STATIC_DIR = "static";

    @Override
    public HttpResponse handle(HttpRequest request) {
        String urlPath = request.getPath();

        // 1. Убираем "/static/", если запрос пришел через него
        if (urlPath.startsWith("/static/")) {
            urlPath = urlPath.replace("/static/", "");
        }

        // 2. Логика для корня: если путь просто "/" или пустой -> отдаем index.html
        if (urlPath.equals("/") || urlPath.isEmpty()) {
            urlPath = "index.html";
        }

        // 3. Убираем лишний слеш в начале, если он остался (например "/style.css" -> "style.css")
        if (urlPath.startsWith("/")) {
            urlPath = urlPath.substring(1);
        }

        try {
            // Ищем файл в папке static
            Path path = Paths.get(STATIC_DIR, urlPath);

            if (Files.exists(path) && !Files.isDirectory(path)) {
                byte[] fileBytes = Files.readAllBytes(path);

                // Простой детектор типа контента
                String contentType = "text/plain";
                if (urlPath.endsWith(".html")) contentType = "text/html";
                else if (urlPath.endsWith(".css")) contentType = "text/css";
                else if (urlPath.endsWith(".js")) contentType = "application/javascript";
                else if (urlPath.endsWith(".png")) contentType = "image/png";
                else if (urlPath.endsWith(".jpg")) contentType = "image/jpeg";

                return new HttpResponse("200 OK", contentType, fileBytes);
            } else {
                return new HttpResponse("404 Not Found", "text/plain", "File not found: " + urlPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return new HttpResponse("500 Internal Error", "text/plain", "Error reading file");
        }
    }
}