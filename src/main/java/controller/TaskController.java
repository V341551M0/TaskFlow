package controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import dto.TaskDto;
import service.TaskService;

public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    public void registerRoutes(HttpServer server) {
        server.createContext("/api/tasks", this::handleTasks);
        server.createContext("/api/habits", this::handleHabits);
        server.createContext("/api/recurring-tasks", this::handleRecurringTasks);
        server.createContext("/api/dashboard", this::handleDashboard);
    }

    private void handleTasks(HttpExchange exchange) throws IOException {
        handleItemRequest(exchange, "task");
    }

    private void handleHabits(HttpExchange exchange) throws IOException {
        handleItemRequest(exchange, "habit");
    }

    private void handleRecurringTasks(HttpExchange exchange) throws IOException {
        handleItemRequest(exchange, "recurring");
    }

    private void handleDashboard(HttpExchange exchange) throws IOException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tasks", taskService.listTasks());
        payload.put("habits", taskService.listHabits());
        payload.put("recurringTasks", taskService.listRecurringTasks());
        sendJson(exchange, 200, payload);
    }

    private void handleItemRequest(HttpExchange exchange, String type) throws IOException {
        String method = exchange.getRequestMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        if ("GET".equalsIgnoreCase(method)) {
            List<TaskDto> items = switch (type) {
                case "habit" -> taskService.listHabits();
                case "recurring" -> taskService.listRecurringTasks();
                default -> taskService.listTasks();
            };
            sendJson(exchange, 200, items);
            return;
        }

        if ("POST".equalsIgnoreCase(method)) {
            String body = readBody(exchange);
            Map<String, String> data = parseJsonBody(body);
            TaskDto created = taskService.createItem(type, data);
            sendJson(exchange, 201, created);
            return;
        }

        sendJson(exchange, 405, Map.of("message", "Method not allowed"));
    }

    private String readBody(HttpExchange exchange) throws IOException {
        InputStream inputStream = exchange.getRequestBody();
        byte[] bytes = inputStream.readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private Map<String, String> parseJsonBody(String body) {
        Map<String, String> values = new HashMap<>();
        String trimmed = body == null ? "" : body.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            String inner = trimmed.substring(1, trimmed.length() - 1).trim();
            if (!inner.isEmpty()) {
                for (String part : inner.split(",")) {
                    String[] keyValue = part.split(":", 2);
                    if (keyValue.length != 2) {
                        continue;
                    }
                    String key = keyValue[0].trim().replace("\"", "");
                    String value = keyValue[1].trim();
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    values.put(key, value);
                }
            }
        }
        return values;
    }

    private void sendJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        String payload = toJson(body);
        byte[] response = payload.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(statusCode, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private String toJson(Object body) {
        if (body instanceof String s) {
            return "\"" + escape(s) + "\"";
        }
        if (body instanceof Number || body instanceof Boolean) {
            return String.valueOf(body);
        }
        if (body instanceof Map<?, ?> map) {
            StringBuilder builder = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    builder.append(",");
                }
                builder.append("\"").append(escape(String.valueOf(entry.getKey()))).append("\":")
                        .append(toJson(entry.getValue()));
                first = false;
            }
            return builder.append("}").toString();
        }
        if (body instanceof List<?> list) {
            StringBuilder builder = new StringBuilder("[");
            boolean first = true;
            for (Object item : list) {
                if (!first) {
                    builder.append(",");
                }
                builder.append(toJson(item));
                first = false;
            }
            return builder.append("]").toString();
        }
        if (body instanceof TaskDto dto) {
            return "{\"id\":\"" + escape(dto.getId()) + "\",\"name\":\"" + escape(dto.getName()) + "\",\"date\":\"" + escape(dto.getDate()) + "\",\"allDays\":" + dto.isAllDays() + ",\"frequencyPerDay\":\"" + escape(dto.getFrequencyPerDay()) + "\",\"type\":\"" + escape(dto.getType()) + "\"}";
        }
        return "\"\"";
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}