package http;

public class LoginHandler implements Handler {
    @Override
    public HttpResponse handle(HttpRequest request) {
        String greeting = "Welcome to singing up!";
        return new HttpResponse("200 OK", "text/html", greeting);
    }
}

