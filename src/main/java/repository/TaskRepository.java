package repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dto.TaskDto;

public class TaskRepository {
    private final List<TaskDto> tasks = new ArrayList<>();
    private final List<TaskDto> habits = new ArrayList<>();
    private final List<TaskDto> recurringTasks = new ArrayList<>();

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
        TaskDto dto = new TaskDto(UUID.randomUUID().toString().substring(0, 8), name, date, allDays, frequency, type);
        switch (type) {
            case "habit" -> habits.add(dto);
            case "recurring" -> recurringTasks.add(dto);
            default -> tasks.add(dto);
        }
        return dto;
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