package http;

import java.util.HashMap;
import java.util.Map;

public class Router {
    Map<String, Handler> handlers;

    public Router() {
        this.handlers = new HashMap<>();
    }

    public void register(String path, Handler handler) {
        handlers.put(path, handler);
    }

    public HttpResponse handle(HttpRequest  request) {
        String path = request.getPath();
        if (handlers.containsKey(path)) {
            HttpResponse httpResponse = handlers.get(path).handle(request);
            return httpResponse;
        }
        else {
            for (Map.Entry<String, Handler> entry : handlers.entrySet()) {
                if (path.contains(entry.getKey())) {
                    HttpResponse httpResponse = handlers.get(entry.getKey()).handle(request);
                    return httpResponse;
                }
            }
        }
        return new HttpResponse("404 Not Found", "text/html", "Error");
    }
}
