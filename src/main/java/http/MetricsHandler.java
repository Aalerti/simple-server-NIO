package http;


import utils.Metrics;

import java.io.IOException;

public class MetricsHandler implements Handler {
    @Override
    public HttpResponse handle(HttpRequest exchange) {
        String response = Metrics.registry.scrape();
        return new HttpResponse("200 OK", "text/plain; version=0.0.4; charset=utf-8", response);
    }
}
