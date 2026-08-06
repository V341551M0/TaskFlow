package service;

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

        return repository.saveItem(type, name, date, allDays, frequency);
    }
}