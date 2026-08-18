package controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import dto.UserDto;
import service.UserService;
import util.Json;

public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    public void registerRoutes(HttpServer server) {
        server.createContext("/api/auth/login", this::handleLogin);
        server.createContext("/api/auth/register", this::handleRegister);
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) {
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("message", "Method not allowed"));
            return;
        }

        Map<String, String> data = Json.parseObject(readBody(exchange));
        try {
            UserDto user = userService.login(data.get("username"), data.get("password"));
            sendJson(exchange, 200, publicUser(user));
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 401, Map.of("message", ex.getMessage()));
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) {
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("message", "Method not allowed"));
            return;
        }

        Map<String, String> data = Json.parseObject(readBody(exchange));
        try {
            UserDto user = userService.register(data.get("name"), data.get("email"), data.get("password"));
            sendJson(exchange, 201, publicUser(user));
        } catch (IllegalStateException ex) {
            sendJson(exchange, 409, Map.of("message", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 400, Map.of("message", ex.getMessage()));
        }
    }

    private Map<String, String> publicUser(UserDto user) {
        Map<String, String> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("email", user.getEmail());
        result.put("date", user.getDate());
        return result;
    }

    private boolean handleOptions(HttpExchange exchange) throws IOException {
        if (!"OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            return false;
        }
        sendCors(exchange, 204);
        return true;
    }

    private String readBody(HttpExchange exchange) throws IOException {
        InputStream inputStream = exchange.getRequestBody();
        byte[] bytes = inputStream.readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private void sendJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        String payload = Json.toJson(body);
        byte[] response = payload.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(statusCode, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private void sendCors(HttpExchange exchange, int statusCode) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(statusCode, -1);
        exchange.close();
    }
}
