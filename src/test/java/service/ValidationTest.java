package service;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dto.ItemRequest;
import dto.UserDto;
import util.DatabaseConnection;
import util.TestDbSupport;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ValidationTest {
    private TaskService service;
    private String userId;

    @BeforeEach
    void setUp() {
        DatabaseConnection.initialize();
        TestDbSupport.clearTables();

        UserDto user = new UserService().register("validacao", "valida@taskflow.test", "senha123");
        userId = user.getId();
        service = new TaskService();
    }

    @Test
    void shouldRejectInvalidDate() {
        assertThrows(IllegalArgumentException.class, () ->
                service.createItem(userId, "task", ItemRequest.from(Map.of(
                        "nome", "Sem data válida",
                        "data", "30/02/2026"
                ))));
        assertThrows(IllegalArgumentException.class, () ->
                service.createItem(userId, "task", ItemRequest.from(Map.of(
                        "nome", "Sem data válida",
                        "data", "qualquer-coisa"
                ))));
    }

    @Test
    void shouldRejectBlankName() {
        assertThrows(IllegalArgumentException.class, () ->
                service.createItem(userId, "task", ItemRequest.from(Map.of(
                        "nome", "   ",
                        "data", "2026-08-07"
                ))));
    }

    @Test
    void shouldRejectInvalidFrequency() {
        assertThrows(IllegalArgumentException.class, () ->
                service.createItem(userId, "task", ItemRequest.from(Map.of(
                        "nome", "Sem frequência",
                        "data", "2026-08-07",
                        "vezesAoDia", "abc"
                ))));
    }

    @Test
    void shouldRejectInvalidItemType() {
        assertThrows(IllegalArgumentException.class, () ->
                service.toggleCompletion(userId, "qualquer-id", "categoria-desconhecida", "2026-08-07"));
        assertThrows(IllegalArgumentException.class, () ->
                service.updateStatus(userId, "qualquer-id", "xpto", "2026-08-07", "completed"));
        assertThrows(IllegalArgumentException.class, () ->
                service.deleteItem(userId, "qualquer-id", "outra"));
    }

    @Test
    void shouldRejectInvalidStatus() {
        assertThrows(IllegalArgumentException.class, () ->
                service.updateStatus(userId, "qualquer-id", "task", "2026-08-07", "talvez"));
        assertThrows(IllegalArgumentException.class, () ->
                service.updateStatus(userId, "qualquer-id", "task", "2026-08-07", ""));
    }

    @Test
    void shouldRejectInvalidDateOnToggleAndStatus() {
        assertThrows(IllegalArgumentException.class, () ->
                service.toggleCompletion(userId, "qualquer-id", "task", "01-01-2026"));
        assertThrows(IllegalArgumentException.class, () ->
                service.updateStatus(userId, "qualquer-id", "task", "não-é-data", "completed"));
    }
}