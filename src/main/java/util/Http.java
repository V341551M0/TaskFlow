package util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;

public final class Http {
    private static final java.util.Set<String> ALLOWED_ORIGINS = java.util.Set.copyOf(
            java.util.Arrays.stream(getEnvOrDefault("CORS_ALLOWED_ORIGINS",
                            "http://localhost:5501,http://127.0.0.1:5501")
                    .split(","))
                    .map(String::trim)
                    .filter(origin -> !origin.isBlank())
                    .toList()
    );

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

    /**
     * CORS restrito às origens permitidas (variável CORS_ALLOWED_ORIGINS,
     * lista separada por vírgula). Apenas a origem da requisição, se permitida,
     * é refletida em Access-Control-Allow-Origin — nunca "*".
     */
    public static void addCors(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", origin);
            exchange.getResponseHeaders().add("Vary", "Origin");
        }
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.getResponseHeaders().add("Access-Control-Expose-Headers", "Authorization");
        exchange.getResponseHeaders().add("Access-Control-Max-Age", "86400");
    }

    private static String getEnvOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }
}