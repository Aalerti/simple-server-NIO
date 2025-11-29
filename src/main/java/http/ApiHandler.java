package http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ApiHandler implements Handler {

    private static final Gson gson = new Gson();

    @Override
    public HttpResponse handle(HttpRequest request) {
        HttpMethods method = request.getMethod();


        if (method == HttpMethods.GET) {
            return new HttpResponse("200 OK", "text/html", "{\"message\": \"Welcome to my api, It was a get request\"}");
        } else if (method == HttpMethods.POST) {

            byte[] body = request.getBody();
            if (body == null || body.length == 0) {
                return new HttpResponse("400 Bad Request", "application/json", "{\"error\": \"Empty body\"}");
            }

            Map<String, String> headers = request.getHeaders();
            String contentType = headers.get("Content-Type");

            if (contentType != null && contentType.contains("application/json")) {
                String bodyString = new String(body, StandardCharsets.UTF_8);

                try {
                    JsonObject jsonRequest = JsonParser.parseString(bodyString).getAsJsonObject();

                    JsonObject jsonResponse = new JsonObject();
                    jsonResponse.addProperty("status", "created");
                    jsonResponse.add("received_data", jsonRequest);

                    return new HttpResponse("200 OK", "application/json", gson.toJson(jsonResponse));

                } catch (JsonSyntaxException e) {
                    return new HttpResponse("400 Bad Request", "application/json", "{\"error\": \"Invalid JSON syntax\"}");
                }
            }
            else {
                // Если прислали Form-Data или картинку в POST
                return new HttpResponse("415 Unsupported Media Type", "text/plain", "Only application/json supported");
            }
        }
        return new HttpResponse("405 Method Not Allowed", "text/plain", "Method Not Allowed");
    }
}
