package service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dto.ItemRequest;
import dto.TaskDto;
import dto.UserDto;
import util.DatabaseConnection;
import util.TestDbSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConcurrencyTest {
    private TaskService service;
    private String userId;

    @BeforeEach
    void setUp() {
        DatabaseConnection.initialize();
        TestDbSupport.clearTables();

        UserDto user = new UserService().register("concorrente", "conc@taskflow.test", "senha123");
        userId = user.getId();
        service = new TaskService();
    }

    @Test
    void shouldSumHeatmapAtomicallyUnderConcurrentCompletions() throws Exception {
        String date = "2026-08-07";
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            TaskDto created = service.createItem(userId, "task", ItemRequest.from(Map.of(
                    "nome", "Item " + i,
                    "data", date,
                    "todosOsDias", "false",
                    "vezesAoDia", "1"
            )));
            ids.add(created.getId());
        }

        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = ids.stream().map(id -> pool.submit(() -> {
            start.await();
            service.toggleCompletion(userId, id, "task", date);
            return true;
        })).toList();
        start.countDown();

        int successes = 0;
        for (Future<Boolean> future : futures) {
            if (future.get()) {
                successes++;
            }
        }
        pool.shutdown();

        assertEquals(8, successes, "todas as conclusões devem ter sucesso");
        assertEquals(8, service.getDailyHeatmap(userId).get(date), "heatmap deve somar todas as contribuições sem perda");
    }

    @Test
    void shouldAllowOnlyOneConcurrentToggleOfSameItem() throws Exception {
        String date = "2026-08-07";
        TaskDto item = service.createItem(userId, "habit", ItemRequest.from(Map.of(
                "nome", "Item único",
                "data", date,
                "todosOsDias", "false",
                "vezesAoDia", "1"
        )));

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    service.toggleCompletion(userId, item.getId(), "habit", date);
                    return true;
                } catch (IllegalStateException expected) {
                    return false;
                }
            }));
        }
        start.countDown();

        int successes = 0;
        for (Future<Boolean> future : futures) {
            if (future.get()) {
                successes++;
            }
        }
        pool.shutdown();

        assertEquals(1, successes, "apenas uma conclusão deve prevalecer para o mesmo item");
        assertEquals(1, service.getDailyHeatmap(userId).get(date));
        TaskDto updated = service.findItemById(userId, item.getId(), "habit");
        assertTrue(updated.isCompletedToday());
    }
}