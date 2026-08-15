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
import util.Json;

public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    public void registerRoutes(HttpServer server) {
        server.createContext("/api/tasks", this::handleTasks);
        server.createContext("/api/habits", this::handleHabits);
        server.createContext("/api/recurring-tasks", this::handleRecurringTasks);
        server.createContext("/api/complete", this::handleComplete);
        server.createContext("/api/delete", this::handleDelete);
        server.createContext("/api/dashboard", this::handleDashboard);
        server.createContext("/api/heatmap", this::handleHeatmap);
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

    private void handleComplete(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            sendCors(exchange, 204);
            return;
        }

        if ("POST".equalsIgnoreCase(method)) {
            String body = readBody(exchange);
            Map<String, String> data = Json.parseObject(body);
            String status = data.get("status");
            TaskDto updated;
            try {
                if (status != null && !status.isBlank()) {
                    updated = taskService.updateStatus(data.get("id"), data.get("type"), data.get("date"), status);
                } else {
                    updated = taskService.toggleCompletion(data.get("id"), data.get("type"), data.get("date"));
                }
                if (updated == null) {
                    sendJson(exchange, 404, Map.of("message", "Item não encontrado"));
                    return;
                }
                sendJson(exchange, 200, updated);
                return;
            } catch (IllegalStateException ex) {
                sendJson(exchange, 400, Map.of("message", ex.getMessage()));
                return;
            }
        }

        sendJson(exchange, 405, Map.of("message", "Method not allowed"));
    }

    private void handleDelete(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            sendCors(exchange, 204);
            return;
        }

        if ("POST".equalsIgnoreCase(method)) {
            String body = readBody(exchange);
            Map<String, String> data = Json.parseObject(body);
            TaskDto deleted = taskService.deleteItem(data.get("id"), data.get("type"));
            if (deleted == null) {
                sendJson(exchange, 404, Map.of("message", "Item não encontrado"));
                return;
            }
            sendJson(exchange, 200, deleted);
            return;
        }

        sendJson(exchange, 405, Map.of("message", "Method not allowed"));
    }

    private void handleDashboard(HttpExchange exchange) throws IOException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tasks", taskService.listTasks());
        payload.put("habits", taskService.listHabits());
        payload.put("recurringTasks", taskService.listRecurringTasks());
        payload.put("heatmap", taskService.getDailyHeatmap());
        sendJson(exchange, 200, payload);
    }

    private void handleHeatmap(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendCors(exchange, 204);
            return;
        }
        sendJson(exchange, 200, taskService.getDailyHeatmap());
    }

    private void handleItemRequest(HttpExchange exchange, String type) throws IOException {
        String method = exchange.getRequestMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            sendCors(exchange, 204);
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
            Map<String, String> data = Json.parseObject(body);
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

    private void sendCors(HttpExchange exchange, int statusCode) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(statusCode, -1);
        exchange.close();
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
}