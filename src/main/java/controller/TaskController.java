package controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import dto.TaskDto;
import service.TaskService;
import util.AuthContext;
import util.AuthFilter;
import util.Http;
import util.Json;

public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    public void registerRoutes(HttpServer server, AuthFilter authFilter) {
        createContext(server, authFilter, "/api/tasks", this::handleTasks);
        createContext(server, authFilter, "/api/habits", this::handleHabits);
        createContext(server, authFilter, "/api/recurring-tasks", this::handleRecurringTasks);
        createContext(server, authFilter, "/api/complete", this::handleComplete);
        createContext(server, authFilter, "/api/delete", this::handleDelete);
        createContext(server, authFilter, "/api/dashboard", this::handleDashboard);
        createContext(server, authFilter, "/api/heatmap", this::handleHeatmap);
    }

    private void createContext(HttpServer server, AuthFilter authFilter, String path, HttpHandler handler) {
        HttpContext context = server.createContext(path, handler);
        context.getFilters().add(authFilter);
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
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            Http.sendCors(exchange, 204);
            return;
        }

        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            String userId = AuthContext.userId();
            try {
                Map<String, Object> data = Json.parseObject(Http.readBody(exchange));
                String status = stringValue(data.get("status"));
                TaskDto updated;
                if (status != null && !status.isBlank()) {
                    updated = taskService.updateStatus(userId, stringValue(data.get("id")), stringValue(data.get("type")), stringValue(data.get("date")), status);
                } else {
                    updated = taskService.toggleCompletion(userId, stringValue(data.get("id")), stringValue(data.get("type")), stringValue(data.get("date")));
                }
                if (updated == null) {
                    Http.sendJson(exchange, 404, Map.of("message", "Item não encontrado"));
                    return;
                }
                Http.sendJson(exchange, 200, updated);
                return;
            } catch (IllegalStateException ex) {
                Http.sendJson(exchange, 400, Map.of("message", ex.getMessage()));
                return;
            } catch (IllegalArgumentException ex) {
                Http.sendJson(exchange, 400, Map.of("message", ex.getMessage()));
                return;
            }
        }

        Http.sendJson(exchange, 405, Map.of("message", "Method not allowed"));
    }

    private void handleDelete(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            Http.sendCors(exchange, 204);
            return;
        }

        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            String userId = AuthContext.userId();
            try {
                Map<String, Object> data = Json.parseObject(Http.readBody(exchange));
                TaskDto deleted = taskService.deleteItem(userId, stringValue(data.get("id")), stringValue(data.get("type")));
                if (deleted == null) {
                    Http.sendJson(exchange, 404, Map.of("message", "Item não encontrado"));
                    return;
                }
                Http.sendJson(exchange, 200, deleted);
                return;
            } catch (IllegalArgumentException ex) {
                Http.sendJson(exchange, 400, Map.of("message", ex.getMessage()));
                return;
            }
        }

        Http.sendJson(exchange, 405, Map.of("message", "Method not allowed"));
    }

    private void handleDashboard(HttpExchange exchange) throws IOException {
        String userId = AuthContext.userId();
        Map<String, Object> payload = new HashMap<>();
        payload.put("tasks", taskService.listTasks(userId));
        payload.put("habits", taskService.listHabits(userId));
        payload.put("recurringTasks", taskService.listRecurringTasks(userId));
        payload.put("heatmap", taskService.getDailyHeatmap(userId));
        Http.sendJson(exchange, 200, payload);
    }

    private void handleHeatmap(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            Http.sendCors(exchange, 204);
            return;
        }
        Http.sendJson(exchange, 200, taskService.getDailyHeatmap(AuthContext.userId()));
    }

    private void handleItemRequest(HttpExchange exchange, String type) throws IOException {
        String method = exchange.getRequestMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            Http.sendCors(exchange, 204);
            return;
        }

        if ("GET".equalsIgnoreCase(method)) {
            String userId = AuthContext.userId();
            List<TaskDto> items = switch (type) {
                case "habit" -> taskService.listHabits(userId);
                case "recurring" -> taskService.listRecurringTasks(userId);
                default -> taskService.listTasks(userId);
            };
            Http.sendJson(exchange, 200, items);
            return;
        }

        if ("POST".equalsIgnoreCase(method)) {
            String userId = AuthContext.userId();
            try {
                Map<String, Object> data = Json.parseObject(Http.readBody(exchange));
                TaskDto created = taskService.createItem(userId, type, dto.ItemRequest.from(data));
                Http.sendJson(exchange, 201, created);
            } catch (IllegalArgumentException ex) {
                Http.sendJson(exchange, 400, Map.of("message", ex.getMessage()));
            }
            return;
        }

        Http.sendJson(exchange, 405, Map.of("message", "Method not allowed"));
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }
}