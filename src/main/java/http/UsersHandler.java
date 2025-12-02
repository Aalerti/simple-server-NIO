package http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import database.UserRepository;
import model.User;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class UsersHandler implements Handler {

    private static final UserRepository userRepository = new UserRepository();
    private static final Gson gson = new Gson();

    private static class UserResponse {
        String status;
        User user;

        public UserResponse(String status, User user) {
            this.status = status;
            this.user = user;
        }
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        HttpMethods method = request.getMethod();


        if (method == HttpMethods.GET) {

            List<User> users = userRepository.findAll();
            String jsonOutput = gson.toJson(users);
            return new HttpResponse("200 OK", "application/json", jsonOutput);

        } else if (method == HttpMethods.POST) {

            byte[] body = request.getBody();
            if (body == null || body.length == 0) {
                return new HttpResponse("400 Bad Request", "application/json", "{\"error\": \"Empty body\"}");
            }

            Map<String, String> headers = request.getHeaders();
            String contentType = headers.get("Content-Type");

            if (contentType != null && contentType.contains("application/json")) {
                try {
                    String bodyString = new String(body, StandardCharsets.UTF_8);
                    User user = gson.fromJson(bodyString, User.class);
                    userRepository.save(user);

                    String jsonOutput = gson.toJson(new UserResponse("created", user));

                    return new HttpResponse("200 OK", "application/json", jsonOutput);

                } catch (JsonSyntaxException | IllegalArgumentException ex) {
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
