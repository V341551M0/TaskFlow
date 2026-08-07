package repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dto.TaskDto;

public class TaskRepository {
    private final List<TaskDto> tasks = new ArrayList<>();
    private final List<TaskDto> habits = new ArrayList<>();
    private final List<TaskDto> recurringTasks = new ArrayList<>();
    private final Map<String, Integer> dailyHeatmap = new HashMap<>();

    public TaskRepository() {
        seedData();
    }

    public List<TaskDto> findTasks() {
        return new ArrayList<>(tasks);
    }

    public List<TaskDto> findHabits() {
        return new ArrayList<>(habits);
    }

    public List<TaskDto> findRecurringTasks() {
        return new ArrayList<>(recurringTasks);
    }

    public TaskDto saveItem(String type, String name, String date, boolean allDays, String frequency) {
        String normalizedDate = (date == null || date.isBlank()) ? LocalDate.now().toString() : date;
        TaskDto dto = new TaskDto(UUID.randomUUID().toString().substring(0, 8), name, normalizedDate, allDays, frequency, type);
        switch (type) {
            case "habit" -> habits.add(dto);
            case "recurring" -> recurringTasks.add(dto);
            default -> tasks.add(dto);
        }
        return dto;
    }

    public TaskDto findItemById(String id, String type) {
        return switch (type) {
            case "habit" -> findById(habits, id);
            case "recurring" -> findById(recurringTasks, id);
            default -> findById(tasks, id);
        };
    }

    public TaskDto deleteItem(String id, String type) {
        TaskDto item = findItemById(id, type);
        if (item == null) {
            return null;
        }

        item.getHistory().forEach((date, contribution) -> {
            int current = dailyHeatmap.getOrDefault(date, 0);
            int next = current - contribution;
            if (next <= 0) {
                dailyHeatmap.remove(date);
            } else {
                dailyHeatmap.put(date, next);
            }
        });

        switch (type) {
            case "habit" -> habits.remove(item);
            case "recurring" -> recurringTasks.remove(item);
            default -> tasks.remove(item);
        }
        return item;
    }

    public TaskDto toggleCompletion(String id, String type, String date) {
        TaskDto item = findItemById(id, type);
        if (item == null) {
            return null;
        }

        String completionDate = (date == null || date.isBlank()) ? item.getDate() : date;
        if (completionDate == null || completionDate.isBlank()) {
            completionDate = LocalDate.now().toString();
        }

        int contribution = parseFrequency(item.getFrequencyPerDay());
        String previousStatus = item.getStatus();
        if ("completed".equalsIgnoreCase(previousStatus)) {
            item.setCompletedToday(false);
            item.setStatus("pending");
            item.setCompletionCount(Math.max(0, item.getCompletionCount() - contribution));
            dailyHeatmap.put(completionDate, dailyHeatmap.getOrDefault(completionDate, 0) - contribution);
        } else {
            item.setCompletedToday(true);
            item.setStatus("completed");
            item.setCompletionCount(item.getCompletionCount() + contribution);
            dailyHeatmap.put(completionDate, dailyHeatmap.getOrDefault(completionDate, 0) + contribution);
        }
        return item;
    }

    public TaskDto updateStatus(String id, String type, String date, String status) {
        TaskDto item = findItemById(id, type);
        if (item == null) {
            return null;
        }

        String completionDate = (date == null || date.isBlank()) ? item.getDate() : date;
        if (completionDate == null || completionDate.isBlank()) {
            completionDate = LocalDate.now().toString();
        }

        int contribution = parseFrequency(item.getFrequencyPerDay());
        boolean completed = "completed".equalsIgnoreCase(status);
        boolean failed = "failed".equalsIgnoreCase(status);
        String previousStatus = item.getStatus();

        if (completed) {
            item.setCompletedToday(true);
            item.setStatus("completed");
            item.setCompletionCount(item.getCompletionCount() + contribution);
            item.getHistory().put(completionDate, contribution);
            dailyHeatmap.put(completionDate, dailyHeatmap.getOrDefault(completionDate, 0) + contribution);
        } else if (failed) {
            item.setCompletedToday(false);
            item.setStatus("failed");
            item.setCompletionCount(Math.max(0, item.getCompletionCount() - contribution));
            item.getHistory().put(completionDate, -contribution);
            dailyHeatmap.put(completionDate, dailyHeatmap.getOrDefault(completionDate, 0) - contribution);
        } else {
            item.setCompletedToday(false);
            item.setStatus("pending");
            item.setCompletionCount(Math.max(0, item.getCompletionCount() - contribution));
            if ("completed".equalsIgnoreCase(previousStatus)) {
                item.getHistory().remove(completionDate);
                dailyHeatmap.put(completionDate, dailyHeatmap.getOrDefault(completionDate, 0) - contribution);
            } else if ("failed".equalsIgnoreCase(previousStatus)) {
                item.getHistory().remove(completionDate);
                dailyHeatmap.put(completionDate, dailyHeatmap.getOrDefault(completionDate, 0) + contribution);
            }
        }

        return item;
    }

    public Map<String, Integer> getDailyHeatmap() {
        return new HashMap<>(dailyHeatmap);
    }

    private TaskDto findById(List<TaskDto> items, String id) {
        return items.stream().filter(item -> id.equals(item.getId())).findFirst().orElse(null);
    }

    private int parseFrequency(String frequency) {
        try {
            return Math.max(1, Integer.parseInt(frequency));
        } catch (Exception ex) {
            return 1;
        }
    }

    private void seedData() {
        saveItem("task", "Estudar Java", "2026-08-06", false, "2");
        saveItem("task", "Revisar projeto", "2026-08-07", true, "1");
        saveItem("habit", "Beber 2L de água", "2026-08-06", true, "3");
        saveItem("habit", "Meditar 10 minutos", "2026-08-06", true, "1");
        saveItem("recurring", "Tarefa semanal", "2026-08-08", true, "1");
        saveItem("recurring", "Limpar workspace", "2026-08-06", false, "2");
    }
}