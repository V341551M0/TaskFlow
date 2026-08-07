package service;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import dto.TaskDto;

public class TaskServiceTest {

    @Test
    void shouldCreateItemAndUpdateDailyHeatmapWhenCompleted() {
        TaskService service = new TaskService();

        TaskDto created = service.createItem("habit", Map.of(
                "nome", "Beber água",
                "data", "2026-08-07",
                "todosOsDias", "true",
                "vezesAoDia", "2"
        ));

        assertTrue(service.listHabits().stream().anyMatch(item -> "Beber água".equals(item.getName())));
        assertEquals("Beber água", created.getName());

        service.toggleCompletion(created.getId(), "habit", "2026-08-07");

        Map<String, Integer> heatmap = service.getDailyHeatmap();
        assertEquals(2, heatmap.get("2026-08-07"));

        TaskDto updated = service.findItemById(created.getId(), "habit");
        assertTrue(updated.isCompletedToday());
    }

    @Test
    void shouldDeleteItemFromTheCorrectList() {
        TaskService service = new TaskService();

        TaskDto created = service.createItem("task", Map.of(
                "nome", "Remover depois",
                "data", "2026-08-07",
                "todosOsDias", "false",
                "vezesAoDia", "1"
        ));

        TaskDto deleted = service.deleteItem(created.getId(), "task");

        assertEquals(created.getId(), deleted.getId());
        assertTrue(service.listTasks().stream().noneMatch(item -> created.getId().equals(item.getId())));
    }
}
