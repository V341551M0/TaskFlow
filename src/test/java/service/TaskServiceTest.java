package service;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dto.ItemRequest;
import dto.TaskDto;
import dto.UserDto;
import util.DatabaseConnection;
import util.TestDbSupport;

public class TaskServiceTest {
    private TaskService service;
    private String userId;

    @BeforeEach
    void setUp() {
        DatabaseConnection.initialize();
        TestDbSupport.clearTables();

        UserDto user = new UserService().register("teste", "teste@taskflow.test", "senha123");
        userId = user.getId();

        service = new TaskService();
    }

    @Test
    void shouldCreateItemAndUpdateDailyHeatmapWhenCompleted() {
        TaskDto created = service.createItem(userId, "habit", ItemRequest.from(Map.of(
                "nome", "Beber água",
                "data", "2026-08-07",
                "todosOsDias", "true",
                "vezesAoDia", "2"
        )));

        assertTrue(service.listHabits(userId).stream().anyMatch(item -> "Beber água".equals(item.getName())));
        assertEquals("Beber água", created.getName());

        service.toggleCompletion(userId, created.getId(), "habit", "2026-08-07");

        Map<String, Integer> heatmap = service.getDailyHeatmap(userId);
        assertEquals(2, heatmap.get("2026-08-07"));

        TaskDto updated = service.findItemById(userId, created.getId(), "habit");
        assertTrue(updated.isCompletedToday());
    }

    @Test
    void shouldDeleteItemFromTheCorrectList() {
        TaskDto created = service.createItem(userId, "task", ItemRequest.from(Map.of(
                "nome", "Remover depois",
                "data", "2026-08-07",
                "todosOsDias", "false",
                "vezesAoDia", "1"
        )));

        TaskDto deleted = service.deleteItem(userId, created.getId(), "task");

        assertEquals(created.getId(), deleted.getId());
        assertTrue(service.listTasks(userId).stream().noneMatch(item -> created.getId().equals(item.getId())));
    }

    @Test
    void shouldPreventStatusChangeWhenAlreadyFinalized() {
        TaskDto created = service.createItem(userId, "task", ItemRequest.from(Map.of(
                "nome", "Validar fluxo",
                "data", "2026-08-07",
                "todosOsDias", "false",
                "vezesAoDia", "2"
        )));

        service.updateStatus(userId, created.getId(), "task", "2026-08-07", "completed");

        TaskDto updated = service.findItemById(userId, created.getId(), "task");
        assertEquals("completed", updated.getStatus());

        // Tentativa de alterar status de item já concluído deve lançar IllegalStateException
        assertThrows(IllegalStateException.class, () -> {
            service.updateStatus(userId, created.getId(), "task", "2026-08-07", "failed");
        });
    }

    @Test
    void shouldRemoveHeatmapHistoryWhenItemIsDeleted() {
        TaskDto created = service.createItem(userId, "habit", ItemRequest.from(Map.of(
                "nome", "Remover histórico",
                "data", "2026-08-07",
                "todosOsDias", "false",
                "vezesAoDia", "3"
        )));

        service.updateStatus(userId, created.getId(), "habit", "2026-08-07", "completed");
        service.deleteItem(userId, created.getId(), "habit");

        Map<String, Integer> heatmap = service.getDailyHeatmap(userId);
        assertEquals(0, heatmap.getOrDefault("2026-08-07", 0));
    }

    @Test
    void shouldRemoveHeatmapHistoryWhenRecurringTaskIsDeletedAfterToggle() {
        TaskDto created = service.createItem(userId, "recurring", ItemRequest.from(Map.of(
                "nome", "Recorrente Teste",
                "data", "2026-08-10",
                "todosOsDias", "true",
                "vezesAoDia", "2"
        )));

        service.toggleCompletion(userId, created.getId(), "recurring", "2026-08-10");
        assertEquals(2, service.getDailyHeatmap(userId).get("2026-08-10"));

        service.deleteItem(userId, created.getId(), "recurring");
        assertEquals(0, service.getDailyHeatmap(userId).getOrDefault("2026-08-10", 0));
        assertTrue(service.listRecurringTasks(userId).stream().noneMatch(item -> created.getId().equals(item.getId())));
    }

    @Test
    void shouldNotExposeItemsOfAnotherUser() {
        TaskDto created = service.createItem(userId, "task", ItemRequest.from(Map.of(
                "nome", "Item privado",
                "data", "2026-08-07",
                "todosOsDias", "false",
                "vezesAoDia", "1"
        )));

        UserDto otherUser = new UserService().register("outro", "outro@taskflow.test", "senha123");
        TaskService otherService = new TaskService();

        assertTrue(otherService.listTasks(otherUser.getId()).stream()
                .noneMatch(item -> created.getId().equals(item.getId())));
        assertEquals(null, otherService.findItemById(otherUser.getId(), created.getId(), "task"));
    }

    @Test
    void shouldLeaveNoSideEffectsWhenFinalizedItemRejectsNewStatus() {
        TaskDto created = service.createItem(userId, "habit", ItemRequest.from(Map.of(
                "nome", "Sem efeitos colaterais",
                "data", "2026-08-07",
                "todosOsDias", "false",
                "vezesAoDia", "2"
        )));

        service.updateStatus(userId, created.getId(), "habit", "2026-08-07", "completed");
        assertEquals(2, service.getDailyHeatmap(userId).get("2026-08-07"));

        // Item finalizado: nova mudança de status e novo toggle devem falhar
        // sem alterar heatmap/histórico/contagem.
        assertThrows(IllegalStateException.class, () ->
                service.updateStatus(userId, created.getId(), "habit", "2026-08-07", "failed"));
        assertThrows(IllegalStateException.class, () ->
                service.toggleCompletion(userId, created.getId(), "habit", "2026-08-07"));

        TaskDto updated = service.findItemById(userId, created.getId(), "habit");
        assertEquals("completed", updated.getStatus());
        assertEquals(2, updated.getCompletionCount());
        assertEquals(2, service.getDailyHeatmap(userId).get("2026-08-07"));
        assertTrue(updated.getHistory().containsKey("2026-08-07"));
        assertEquals(2, updated.getHistory().get("2026-08-07"));
    }
}