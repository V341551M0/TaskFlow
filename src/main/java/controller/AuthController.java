package controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import dto.UserDto;
import service.UserService;
import util.AuthFilter;
import util.Http;
import util.Json;
import util.TokenService;

public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    public void registerRoutes(HttpServer server, AuthFilter authFilter) {
        createContext(server, authFilter, "/api/auth/login", this::handleLogin);
        createContext(server, authFilter, "/api/auth/register", this::handleRegister);
    }

    private void createContext(HttpServer server, AuthFilter authFilter, String path, HttpHandler handler) {
        HttpContext context = server.createContext(path, handler);
        context.getFilters().add(authFilter);
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) {
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            Http.sendJson(exchange, 405, Map.of("message", "Method not allowed"));
            return;
        }

        try {
            Map<String, Object> data = Json.parseObject(Http.readBody(exchange));
            UserDto user = userService.login(stringValue(data.get("username")), stringValue(data.get("password")));
            Http.sendJson(exchange, 200, withToken(user));
        } catch (IllegalArgumentException ex) {
            Http.sendJson(exchange, 401, Map.of("message", ex.getMessage()));
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) {
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            Http.sendJson(exchange, 405, Map.of("message", "Method not allowed"));
            return;
        }

        try {
            Map<String, Object> data = Json.parseObject(Http.readBody(exchange));
            UserDto user = userService.register(
                    stringValue(data.get("name")),
                    stringValue(data.get("email")),
                    stringValue(data.get("password"))
            );
            Http.sendJson(exchange, 201, withToken(user));
        } catch (IllegalStateException ex) {
            Http.sendJson(exchange, 409, Map.of("message", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            Http.sendJson(exchange, 400, Map.of("message", ex.getMessage()));
        }
    }

    private Map<String, String> withToken(UserDto user) {
        Map<String, String> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("email", user.getEmail());
        result.put("date", user.getDate());
        result.put("token", TokenService.generate(user.getId()));
        return result;
    }

    private boolean handleOptions(HttpExchange exchange) throws IOException {
        if (!"OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            return false;
        }
        Http.sendCors(exchange, 204);
        return true;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}