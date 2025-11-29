package http;

public class MainPageHandler implements Handler {
    @Override
    public HttpResponse handle(HttpRequest request) {
        String greeting = "Welcome to my Java server!";
        return new HttpResponse("200 OK", "text/html", greeting);
    }
}

