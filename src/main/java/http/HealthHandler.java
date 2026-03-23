package http;

public class HealthHandler implements Handler {
    @Override
    public HttpResponse handle(HttpRequest  request) {
        return new HttpResponse("200 OK", "application/json", "{\"status\": \"UP\"}");
    }
}
