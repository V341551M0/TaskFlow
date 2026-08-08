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

    @Test
    void shouldUpdateHeatmapWhenAnItemIsCompletedAndThenMarkedAsFailed() {
        TaskService service = new TaskService();

        TaskDto created = service.createItem("task", Map.of(
                "nome", "Validar fluxo",
                "data", "2026-08-07",
                "todosOsDias", "false",
                "vezesAoDia", "2"
        ));

        service.updateStatus(created.getId(), "task", "2026-08-07", "completed");
        service.updateStatus(created.getId(), "task", "2026-08-07", "failed");

        Map<String, Integer> heatmap = service.getDailyHeatmap();
        assertEquals(0, heatmap.getOrDefault("2026-08-07", 0));

        TaskDto updated = service.findItemById(created.getId(), "task");
        assertEquals("failed", updated.getStatus());
    }

    @Test
    void shouldRemoveHeatmapHistoryWhenItemIsDeleted() {
        TaskService service = new TaskService();

        TaskDto created = service.createItem("habit", Map.of(
                "nome", "Remover histórico",
                "data", "2026-08-07",
                "todosOsDias", "false",
                "vezesAoDia", "3"
        ));

        service.updateStatus(created.getId(), "habit", "2026-08-07", "completed");
        service.deleteItem(created.getId(), "habit");

        Map<String, Integer> heatmap = service.getDailyHeatmap();
        assertEquals(0, heatmap.getOrDefault("2026-08-07", 0));
    }

    @Test
    void shouldRemoveHeatmapHistoryWhenRecurringTaskIsDeletedAfterToggle() {
        TaskService service = new TaskService();

        TaskDto created = service.createItem("recurring", Map.of(
                "nome", "Recorrente Teste",
                "data", "2026-08-10",
                "todosOsDias", "true",
                "vezesAoDia", "2"
        ));

        service.toggleCompletion(created.getId(), "recurring", "2026-08-10");
        assertEquals(2, service.getDailyHeatmap().get("2026-08-10"));

        service.deleteItem(created.getId(), "recurring");
        assertEquals(0, service.getDailyHeatmap().getOrDefault("2026-08-10", 0));
        assertTrue(service.listRecurringTasks().stream().noneMatch(item -> created.getId().equals(item.getId())));
    }
}
