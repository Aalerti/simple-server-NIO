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

            String path = request.getPath();
            long userId = parseIdFromPath(path);

            if (userId == 0) {
                try {
                    List<User> users = userRepository.findAll();
                    String jsonOutput = gson.toJson(users);
                    return new HttpResponse("200 OK", "application/json", jsonOutput);
                } catch (RuntimeException e) {
                    return new HttpResponse("500 Internal Server Error", "application/json", "{\"error\": \"Database error\"}");
                }
            }
            else {
                try {
                    User user = userRepository.findUserById(userId);
                    String jsonOutput = gson.toJson(user);
                    return new HttpResponse("200 OK", "application/json", jsonOutput);
                } catch (RuntimeException e) {
                    return new HttpResponse("404 Not Found", "application/json", "{\"error\": \"User not found\"}");
                }
            }

        } else if (method == HttpMethods.POST || method == HttpMethods.PUT) {

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
                    String status;

                    if (method == HttpMethods.PUT) {
                        long userId = parseIdFromPath(request.getPath());
                        user.setId(userId);
                        userRepository.update(user);
                        status = "updated";
                    }
                    else {
                        userRepository.save(user);
                        status = "created";
                    }

                    String jsonOutput = gson.toJson(new UserResponse("created", user));
                    String code = (method == HttpMethods.POST) ? "201 Created" : "200 OK";

                    return new HttpResponse(code, "application/json", jsonOutput);

                } catch (JsonSyntaxException | IllegalArgumentException ex) {
                    return new HttpResponse("400 Bad Request", "application/json", "{\"error\": \"Invalid JSON syntax\"}");
                } catch (RuntimeException ex) {
                    return new HttpResponse("500 Internal Server Error", "application/json", "{\"error\": \"Database error\"}");
                }
            }
            else {
                return new HttpResponse("415 Unsupported Media Type", "text/plain", "Only application/json supported");
            }


        } else if (method == HttpMethods.DELETE) {


            try {
                String path = request.getPath();
                long userId = parseIdFromPath(path);
                User user = userRepository.findUserById(userId);
                userRepository.delete(userId);

                String jsonOutput = gson.toJson(new UserResponse("delete", user));

                return new HttpResponse("200 OK", "application/json", jsonOutput);

            } catch (NumberFormatException e) {
                return new HttpResponse("400 Bad Request", "application/json", "{\"error\": \"Invalid path\"}");
            } catch (RuntimeException e) {
                return new HttpResponse("404 Not Found", "application/json", "{\"error\": \"User not found\"}");
            }

        }

        return new HttpResponse("405 Method Not Allowed", "text/plain", "Method Not Allowed");
    }

    private long parseIdFromPath(String path) {
        try {
            String[] parts = path.split("/");
            for (int i = parts.length - 1; i >= 0; i--) {
                if (!parts[i].trim().isEmpty()) {
                    return Long.parseLong(parts[i]);
                }
            }
        } catch (Exception e) {
        }
        return 0L;
    }
}
