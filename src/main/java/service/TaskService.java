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

    public List<TaskDto> listTasks() {
        return repository.findTasks();
    }

    public List<TaskDto> listHabits() {
        return repository.findHabits();
    }

    public List<TaskDto> listRecurringTasks() {
        return repository.findRecurringTasks();
    }

    public TaskDto createItem(String type, Map<String, String> data) {
        String name = data.getOrDefault("nome", data.getOrDefault("name", "Novo item"));
        String date = data.getOrDefault("data", data.getOrDefault("date", ""));
        boolean allDays = Boolean.parseBoolean(data.getOrDefault("todosOsDias", data.getOrDefault("allDays", "false")));
        String frequency = data.getOrDefault("vezesAoDia", data.getOrDefault("frequencyPerDay", "1"));

        if (date == null || date.isBlank()) {
            date = LocalDate.now().toString();
        }

        return repository.saveItem(type, name, date, allDays, frequency);
    }

    public TaskDto toggleCompletion(String id, String type, String date) {
        return repository.toggleCompletion(id, type, date);
    }

    public TaskDto updateStatus(String id, String type, String date, String status) {
        return repository.updateStatus(id, type, date, status);
    }

    public TaskDto deleteItem(String id, String type) {
        return repository.deleteItem(id, type);
    }

    public TaskDto findItemById(String id, String type) {
        return repository.findItemById(id, type);
    }

    public Map<String, Integer> getDailyHeatmap() {
        return repository.getDailyHeatmap();
    }
}