package util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;

public final class Http {
    private Http() {
    }

    public static void sendJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        String payload = Json.toJson(body);
        byte[] response = payload.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        addCors(exchange);
        exchange.sendResponseHeaders(statusCode, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    public static void sendCors(HttpExchange exchange, int statusCode) throws IOException {
        addCors(exchange);
        exchange.sendResponseHeaders(statusCode, -1);
        exchange.close();
    }

    public static String readBody(HttpExchange exchange) throws IOException {
        InputStream inputStream = exchange.getRequestBody();
        byte[] bytes = inputStream.readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static void addCors(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.getResponseHeaders().add("Access-Control-Expose-Headers", "Authorization");
    }
}