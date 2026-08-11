package repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dto.TaskDto;
import util.DatabaseConnection;

public class TaskRepository {

    public TaskRepository() {
        DatabaseConnection.initialize();
        seedDataIfEmpty();
    }

    public List<TaskDto> findTasks() {
        return findByType("task");
    }

    public List<TaskDto> findHabits() {
        return findByType("habit");
    }

    public List<TaskDto> findRecurringTasks() {
        return findByType("recurring");
    }

    public TaskDto saveItem(String type, String name, String date, boolean allDays, String frequency) {
        String normalizedDate = (date == null || date.isBlank()) ? LocalDate.now().toString() : date;
        TaskDto dto = new TaskDto(
                UUID.randomUUID().toString().substring(0, 8),
                name,
                normalizedDate,
                allDays,
                frequency,
                type
        );

        String sql = """
                INSERT INTO %s (id, nome, data, todos_os_dias, vezes_ao_dia, completed_today, completion_count, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.formatted(tableForType(type));

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindItem(statement, dto);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Não foi possível salvar o item", ex);
        }

        return dto;
    }

    public TaskDto findItemById(String id, String type) {
        if (type == null || type.isBlank()) {
            TaskDto item = findById("task", id);
            if (item != null) {
                return item;
            }
            item = findById("habit", id);
            if (item != null) {
                return item;
            }
            return findById("recurring", id);
        }
        return findById(type, id);
    }

    public TaskDto deleteItem(String id, String type) {
        TaskDto item = findItemById(id, type);
        if (item == null) {
            return null;
        }

        String resolvedType = type != null && !type.isBlank() ? type : item.getType();
        Map<String, Integer> history = loadHistory(item.getId(), resolvedType);
        item.setHistory(history);

        if (history != null && !history.isEmpty()) {
            history.forEach((date, contribution) -> adjustHeatmap(date, -contribution));
            deleteHistory(item.getId());
        } else {
            String completionDate = (item.getDate() == null || item.getDate().isBlank())
                    ? LocalDate.now().toString()
                    : item.getDate();
            int contribution = parseFrequency(item.getFrequencyPerDay());
            if ("completed".equalsIgnoreCase(item.getStatus()) || item.isCompletedToday()) {
                adjustHeatmap(completionDate, -contribution);
            } else if ("failed".equalsIgnoreCase(item.getStatus())) {
                adjustHeatmap(completionDate, contribution);
            }
        }

        String sql = "DELETE FROM %s WHERE id = ?".formatted(tableForType(resolvedType));
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, item.getId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Não foi possível excluir o item", ex);
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
            removeHistoryEntry(item.getId(), item.getType(), completionDate);
            adjustHeatmap(completionDate, -contribution);
        } else {
            item.setCompletedToday(true);
            item.setStatus("completed");
            item.setCompletionCount(item.getCompletionCount() + contribution);
            upsertHistoryEntry(item.getId(), item.getType(), completionDate, contribution);
            adjustHeatmap(completionDate, contribution);
        }

        persistItem(item);
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
            upsertHistoryEntry(item.getId(), item.getType(), completionDate, contribution);
            adjustHeatmap(completionDate, contribution);
        } else if (failed) {
            item.setCompletedToday(false);
            item.setStatus("failed");
            item.setCompletionCount(Math.max(0, item.getCompletionCount() - contribution));
            upsertHistoryEntry(item.getId(), item.getType(), completionDate, -contribution);
            adjustHeatmap(completionDate, -contribution);
        } else {
            item.setCompletedToday(false);
            item.setStatus("pending");
            item.setCompletionCount(Math.max(0, item.getCompletionCount() - contribution));
            if ("completed".equalsIgnoreCase(previousStatus)) {
                removeHistoryEntry(item.getId(), item.getType(), completionDate);
                adjustHeatmap(completionDate, -contribution);
            } else if ("failed".equalsIgnoreCase(previousStatus)) {
                removeHistoryEntry(item.getId(), item.getType(), completionDate);
                adjustHeatmap(completionDate, contribution);
            }
        }

        persistItem(item);
        return item;
    }

    public Map<String, Integer> getDailyHeatmap() {
        Map<String, Integer> heatmap = new LinkedHashMap<>();
        String sql = "SELECT date, value FROM daily_heatmap";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                heatmap.put(resultSet.getString("date"), resultSet.getInt("value"));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Não foi possível carregar o heatmap", ex);
        }

        return heatmap;
    }

    private List<TaskDto> findByType(String type) {
        String sql = "SELECT * FROM %s ORDER BY data".formatted(tableForType(type));

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            Map<String, TaskDto> items = new LinkedHashMap<>();
            while (resultSet.next()) {
                TaskDto dto = mapRowToDto(resultSet, type);
                dto.setHistory(loadHistory(dto.getId(), type));
                items.put(dto.getId(), dto);
            }
            return List.copyOf(items.values());
        } catch (SQLException ex) {
            throw new IllegalStateException("Não foi possível listar os itens", ex);
        }
    }

    private TaskDto findById(String type, String id) {
        String sql = "SELECT * FROM %s WHERE id = ?".formatted(tableForType(type));

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                TaskDto dto = mapRowToDto(resultSet, type);
                dto.setHistory(loadHistory(dto.getId(), type));
                return dto;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Não foi possível buscar o item", ex);
        }
    }

    private void persistItem(TaskDto item) {
        String sql = """
                UPDATE %s
                SET nome = ?, data = ?, todos_os_dias = ?, vezes_ao_dia = ?,
                    completed_today = ?, completion_count = ?, status = ?
                WHERE id = ?
                """.formatted(tableForType(item.getType()));

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, item.getName());
            statement.setString(2, item.getDate());
            statement.setInt(3, item.isAllDays() ? 1 : 0);
            statement.setString(4, item.getFrequencyPerDay());
            statement.setInt(5, item.isCompletedToday() ? 1 : 0);
            statement.setInt(6, item.getCompletionCount());
            statement.setString(7, item.getStatus());
            statement.setString(8, item.getId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Não foi possível atualizar o item", ex);
        }
    }

    private Map<String, Integer> loadHistory(String itemId, String type) {
        Map<String, Integer> history = new HashMap<>();
        String sql = "SELECT date, contribution FROM item_history WHERE item_id = ? AND item_type = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, itemId);
            statement.setString(2, type);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    history.put(resultSet.getString("date"), resultSet.getInt("contribution"));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Não foi possível carregar o histórico", ex);
        }

        return history;
    }

    private void upsertHistoryEntry(String itemId, String type, String date, int contribution) {
        String sql = """
                INSERT INTO item_history (item_id, item_type, date, contribution)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(item_id, date) DO UPDATE SET contribution = excluded.contribution
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, itemId);
            statement.setString(2, type);
            statement.setString(3, date);
            statement.setInt(4, contribution);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Não foi possível salvar o histórico", ex);
        }
    }

    private void removeHistoryEntry(String itemId, String type, String date) {
        String sql = "DELETE FROM item_history WHERE item_id = ? AND item_type = ? AND date = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, itemId);
            statement.setString(2, type);
            statement.setString(3, date);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Não foi possível remover o histórico", ex);
        }
    }

    private void deleteHistory(String itemId) {
        String sql = "DELETE FROM item_history WHERE item_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, itemId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Não foi possível limpar o histórico", ex);
        }
    }

    private void adjustHeatmap(String date, int delta) {
        int current = getHeatmapValue(date);
        int next = current + delta;

        if (next <= 0) {
            String deleteSql = "DELETE FROM daily_heatmap WHERE date = ?";
            try (Connection connection = DatabaseConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(deleteSql)) {
                statement.setString(1, date);
                statement.executeUpdate();
            } catch (SQLException ex) {
                throw new IllegalStateException("Não foi possível atualizar o heatmap", ex);
            }
            return;
        }

        String upsertSql = """
                INSERT INTO daily_heatmap (date, value)
                VALUES (?, ?)
                ON CONFLICT(date) DO UPDATE SET value = excluded.value
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(upsertSql)) {
            statement.setString(1, date);
            statement.setInt(2, next);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Não foi possível atualizar o heatmap", ex);
        }
    }

    private int getHeatmapValue(String date) {
        String sql = "SELECT value FROM daily_heatmap WHERE date = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, date);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("value");
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Não foi possível consultar o heatmap", ex);
        }

        return 0;
    }

    private TaskDto mapRowToDto(ResultSet resultSet, String type) throws SQLException {
        TaskDto dto = new TaskDto(
                resultSet.getString("id"),
                resultSet.getString("nome"),
                resultSet.getString("data"),
                resultSet.getInt("todos_os_dias") == 1,
                resultSet.getString("vezes_ao_dia"),
                type
        );
        dto.setCompletedToday(resultSet.getInt("completed_today") == 1);
        dto.setCompletionCount(resultSet.getInt("completion_count"));
        dto.setStatus(resultSet.getString("status"));
        return dto;
    }

    private void bindItem(PreparedStatement statement, TaskDto dto) throws SQLException {
        statement.setString(1, dto.getId());
        statement.setString(2, dto.getName());
        statement.setString(3, dto.getDate());
        statement.setInt(4, dto.isAllDays() ? 1 : 0);
        statement.setString(5, dto.getFrequencyPerDay());
        statement.setInt(6, dto.isCompletedToday() ? 1 : 0);
        statement.setInt(7, dto.getCompletionCount());
        statement.setString(8, dto.getStatus());
    }

    private String tableForType(String type) {
        return switch (type) {
            case "habit" -> "habit";
            case "recurring" -> "recurring_task";
            default -> "task";
        };
    }

    private int parseFrequency(String frequency) {
        try {
            return Math.max(1, Integer.parseInt(frequency));
        } catch (Exception ex) {
            return 1;
        }
    }

    private void seedDataIfEmpty() {
        if (!findTasks().isEmpty() || !findHabits().isEmpty() || !findRecurringTasks().isEmpty()) {
            return;
        }

        saveItem("task", "Estudar Java", "2026-08-06", false, "2");
        saveItem("task", "Revisar projeto", "2026-08-07", true, "1");
        saveItem("habit", "Beber 2L de água", "2026-08-06", true, "3");
        saveItem("habit", "Meditar 10 minutos", "2026-08-06", true, "1");
        saveItem("recurring", "Tarefa semanal", "2026-08-08", true, "1");
        saveItem("recurring", "Limpar workspace", "2026-08-06", false, "2");
    }
}
