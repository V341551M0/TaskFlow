package service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dto.ItemRequest;
import dto.TaskDto;
import repository.TaskRepository;

public class TaskService {
    private static final Set<String> VALID_TYPES = Set.of("task", "habit", "recurring");
    private static final Set<String> VALID_STATUSES = Set.of("pending", "completed", "failed");

    private final TaskRepository repository;

    public TaskService() {
        this.repository = new TaskRepository();
    }

    public List<TaskDto> listTasks(String userId) {
        return repository.findTasks(userId);
    }

    public List<TaskDto> listHabits(String userId) {
        return repository.findHabits(userId);
    }

    public List<TaskDto> listRecurringTasks(String userId) {
        return repository.findRecurringTasks(userId);
    }

    public TaskDto createItem(String userId, String type, ItemRequest request) {
        validateType(type);
        return repository.saveItem(userId, type, request.name(), request.date().toString(), request.allDays(),
                String.valueOf(request.frequencyPerDay()));
    }

    public TaskDto toggleCompletion(String userId, String id, String type, String date) {
        validateType(type);
        return repository.toggleCompletion(userId, id, type, normalizeDate(date));
    }

    public TaskDto updateStatus(String userId, String id, String type, String date, String status) {
        validateType(type);
        if (status == null || !VALID_STATUSES.contains(status.trim().toLowerCase())) {
            throw new IllegalArgumentException("Informe um status válido (pending, completed ou failed).");
        }
        return repository.updateStatus(userId, id, type, normalizeDate(date), status.trim().toLowerCase());
    }

    public TaskDto deleteItem(String userId, String id, String type) {
        validateType(type);
        return repository.deleteItem(userId, id, type);
    }

    public TaskDto findItemById(String userId, String id, String type) {
        return repository.findItemById(userId, id, type);
    }

    public Map<String, Integer> getDailyHeatmap(String userId) {
        return repository.getDailyHeatmap(userId);
    }

    private void validateType(String type) {
        if (type != null && !type.isBlank() && !VALID_TYPES.contains(type.trim().toLowerCase())) {
            throw new IllegalArgumentException("Tipo de atividade inválido (use task, habit ou recurring).");
        }
    }

    private String normalizeDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        return ItemRequest.parseDate(date).toString();
    }
}