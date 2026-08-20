package service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import dto.TaskDto;
import repository.TaskRepository;

public class TaskService {
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

    public TaskDto createItem(String userId, String type, Map<String, Object> data) {
        String name = stringValue(data.getOrDefault("nome", data.getOrDefault("name", "")));
        if (name.isBlank()) {
            throw new IllegalArgumentException("Informe o nome da atividade.");
        }

        String date = stringValue(data.getOrDefault("data", data.getOrDefault("date", "")));
        if (date.isBlank()) {
            date = LocalDate.now().toString();
        }

        boolean allDays = booleanValue(data.getOrDefault("todosOsDias", data.getOrDefault("allDays", false)));
        String frequency = stringValue(data.getOrDefault("vezesAoDia", data.getOrDefault("frequencyPerDay", "1")));
        if (frequency.isBlank() || parseFrequency(frequency) < 1) {
            throw new IllegalArgumentException("Informe uma frequência válida (número de vezes por dia).");
        }

        return repository.saveItem(userId, type, name, date, allDays, frequency);
    }

    public TaskDto toggleCompletion(String userId, String id, String type, String date) {
        return repository.toggleCompletion(userId, id, type, date);
    }

    public TaskDto updateStatus(String userId, String id, String type, String date, String status) {
        return repository.updateStatus(userId, id, type, date, status);
    }

    public TaskDto deleteItem(String userId, String id, String type) {
        return repository.deleteItem(userId, id, type);
    }

    public TaskDto findItemById(String userId, String id, String type) {
        return repository.findItemById(userId, id, type);
    }

    public Map<String, Integer> getDailyHeatmap(String userId) {
        return repository.getDailyHeatmap(userId);
    }

    private String stringValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String string) {
            return string.trim();
        }
        return String.valueOf(value);
    }

    private boolean booleanValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return "true".equalsIgnoreCase(String.valueOf(value));
    }

    private int parseFrequency(String frequency) {
        try {
            return Math.max(1, Integer.parseInt(frequency));
        } catch (NumberFormatException ex) {
            return -1;
        }
    }
}