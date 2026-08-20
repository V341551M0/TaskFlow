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
    }

    public List<TaskDto> findTasks(String userId) {
        return findByType(userId, "task");
    }

    public List<TaskDto> findHabits(String userId) {
        return findByType(userId, "habit");
    }

    public List<TaskDto> findRecurringTasks(String userId) {
        return findByType(userId, "recurring");
    }

    public TaskDto saveItem(String userId, String type, String name, String date, boolean allDays, String frequency) {
        String normalizedDate = (date == null || date.isBlank()) ? LocalDate.now().toString() : date;
        TaskDto dto = new TaskDto(
                UUID.randomUUID().toString().substring(0, 8),
                name,
                normalizedDate,
                allDays,
                frequency,
                type
        );
        dto.setUserId(userId);

        String sql = """
                INSERT INTO %s (id, nome, data, todos_os_dias, vezes_ao_dia, completed_today, completion_count, status, user_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.formatted(tableForType(type));

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindItem(statement, dto);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Não foi possível salvar o item no banco de dados", ex);
        }

        return dto;
    }

    public TaskDto findItemById(String userId, String id, String type) {
        if (type == null || type.isBlank()) {
            TaskDto item = findById(userId, "task", id);
            if (item != null) {
                return item;
            }
            item = findById(userId, "habit", id);
            if (item != null) {
                return item;
            }
            return findById(userId, "recurring", id);
        }
        return findById(userId, type, id);
    }

    public TaskDto deleteItem(String userId, String id, String type) {
        return inTransaction(connection -> {
            TaskDto item = findItemById(connection, userId, id, type);
            if (item == null) {
                return null;
            }

            String resolvedType = type != null && !type.isBlank() ? type : item.getType();
            Map<String, Integer> history = loadHistory(connection, userId, item.getId(), resolvedType);
            item.setHistory(history);

            if (history != null && !history.isEmpty()) {
                for (Map.Entry<String, Integer> entry : history.entrySet()) {
                    adjustHeatmap(connection, userId, entry.getKey(), -entry.getValue());
                }
                deleteHistory(connection, userId, item.getId());
            } else {
                String completionDate = (item.getDate() == null || item.getDate().isBlank())
                        ? LocalDate.now().toString()
                        : item.getDate();
                int contribution = parseFrequency(item.getFrequencyPerDay());
                if ("completed".equalsIgnoreCase(item.getStatus()) || item.isCompletedToday()) {
                    adjustHeatmap(connection, userId, completionDate, -contribution);
                } else if ("failed".equalsIgnoreCase(item.getStatus())) {
                    adjustHeatmap(connection, userId, completionDate, contribution);
                }
            }

            String sql = "DELETE FROM %s WHERE id = ? AND user_id = ?".formatted(tableForType(resolvedType));
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, item.getId());
                statement.setString(2, userId);
                statement.executeUpdate();
            }

            return item;
        });
    }

    public TaskDto toggleCompletion(String userId, String id, String type, String date) {
        return inTransaction(connection -> {
            TaskDto item = findItemById(connection, userId, id, type);
            if (item == null) {
                return null;
            }

            String currentStatus = item.getStatus();
            if ("completed".equalsIgnoreCase(currentStatus) || "failed".equalsIgnoreCase(currentStatus)) {
                throw new IllegalStateException("Atividade já possui status finalizado (concluída ou falha) e não pode ser alterada novamente.");
            }

            String completionDate = (date == null || date.isBlank()) ? item.getDate() : date;
            if (completionDate == null || completionDate.isBlank()) {
                completionDate = LocalDate.now().toString();
            }

            int contribution = parseFrequency(item.getFrequencyPerDay());
            item.setCompletedToday(true);
            item.setStatus("completed");
            item.setCompletionCount(item.getCompletionCount() + contribution);

            upsertHistoryEntry(connection, userId, item.getId(), item.getType(), completionDate, contribution);
            adjustHeatmap(connection, userId, completionDate, contribution);
            persistItem(connection, item);

            return item;
        });
    }

    public TaskDto updateStatus(String userId, String id, String type, String date, String status) {
        return inTransaction(connection -> {
            TaskDto item = findItemById(connection, userId, id, type);
            if (item == null) {
                return null;
            }

            String currentStatus = item.getStatus();
            if ("completed".equalsIgnoreCase(currentStatus) || "failed".equalsIgnoreCase(currentStatus)) {
                if (currentStatus.equalsIgnoreCase(status)) {
                    return item;
                }
                throw new IllegalStateException("Atividade já possui status finalizado (concluída ou falha) e não pode ser alterada novamente.");
            }

            String completionDate = (date == null || date.isBlank()) ? item.getDate() : date;
            if (completionDate == null || completionDate.isBlank()) {
                completionDate = LocalDate.now().toString();
            }

            int contribution = parseFrequency(item.getFrequencyPerDay());
            boolean completed = "completed".equalsIgnoreCase(status);
            boolean failed = "failed".equalsIgnoreCase(status);

            if (completed) {
                item.setCompletedToday(true);
                item.setStatus("completed");
                item.setCompletionCount(item.getCompletionCount() + contribution);
                upsertHistoryEntry(connection, userId, item.getId(), item.getType(), completionDate, contribution);
                adjustHeatmap(connection, userId, completionDate, contribution);
            } else if (failed) {
                item.setCompletedToday(false);
                item.setStatus("failed");
                item.setCompletionCount(Math.max(0, item.getCompletionCount() - contribution));
                upsertHistoryEntry(connection, userId, item.getId(), item.getType(), completionDate, -contribution);
                adjustHeatmap(connection, userId, completionDate, -contribution);
            } else {
                item.setCompletedToday(false);
                item.setStatus("pending");
            }

            persistItem(connection, item);
            return item;
        });
    }

    public Map<String, Integer> getDailyHeatmap(String userId) {
        Map<String, Integer> heatmap = new LinkedHashMap<>();
        String sql = "SELECT date, value FROM daily_heatmap WHERE user_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    heatmap.put(resultSet.getString("date"), resultSet.getInt("value"));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Não foi possível carregar o heatmap", ex);
        }

        return heatmap;
    }

    private List<TaskDto> findByType(String userId, String type) {
        String sql = "SELECT * FROM %s WHERE user_id = ? ORDER BY data".formatted(tableForType(type));

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                Map<String, TaskDto> items = new LinkedHashMap<>();
                while (resultSet.next()) {
                    TaskDto dto = mapRowToDto(resultSet, type);
                    dto.setHistory(loadHistory(connection, userId, dto.getId(), type));
                    items.put(dto.getId(), dto);
                }
                return List.copyOf(items.values());
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Não foi possível listar os itens", ex);
        }
    }

    private TaskDto findById(String userId, String type, String id) {
        String sql = "SELECT * FROM %s WHERE id = ? AND user_id = ?".formatted(tableForType(type));

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.setString(2, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                TaskDto dto = mapRowToDto(resultSet, type);
                dto.setHistory(loadHistory(connection, userId, dto.getId(), type));
                return dto;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Não foi possível buscar o item", ex);
        }
    }

    private TaskDto findItemById(Connection connection, String userId, String id, String type) throws SQLException {
        if (type == null || type.isBlank()) {
            for (String candidate : List.of("task", "habit", "recurring")) {
                TaskDto item = findById(connection, userId, candidate, id);
                if (item != null) {
                    return item;
                }
            }
            return null;
        }
        return findById(connection, userId, type, id);
    }

    private TaskDto findById(Connection connection, String userId, String type, String id) throws SQLException {
        String sql = "SELECT * FROM %s WHERE id = ? AND user_id = ?".formatted(tableForType(type));
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.setString(2, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                TaskDto dto = mapRowToDto(resultSet, type);
                dto.setHistory(loadHistory(connection, userId, dto.getId(), type));
                return dto;
            }
        }
    }

    private void persistItem(Connection connection, TaskDto item) throws SQLException {
        String sql = """
                UPDATE %s
                SET nome = ?, data = ?, todos_os_dias = ?, vezes_ao_dia = ?,
                    completed_today = ?, completion_count = ?, status = ?
                WHERE id = ? AND user_id = ?
                """.formatted(tableForType(item.getType()));

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, item.getName());
            statement.setString(2, item.getDate());
            statement.setInt(3, item.isAllDays() ? 1 : 0);
            statement.setString(4, item.getFrequencyPerDay());
            statement.setInt(5, item.isCompletedToday() ? 1 : 0);
            statement.setInt(6, item.getCompletionCount());
            statement.setString(7, item.getStatus());
            statement.setString(8, item.getId());
            statement.setString(9, item.getUserId());
            statement.executeUpdate();
        }
    }

    private Map<String, Integer> loadHistory(Connection connection, String userId, String itemId, String type) throws SQLException {
        Map<String, Integer> history = new HashMap<>();
        String sql = "SELECT date, contribution FROM item_history WHERE item_id = ? AND item_type = ? AND user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, itemId);
            statement.setString(2, type);
            statement.setString(3, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    history.put(resultSet.getString("date"), resultSet.getInt("contribution"));
                }
            }
        }
        return history;
    }

    private void upsertHistoryEntry(Connection connection, String userId, String itemId, String type, String date, int contribution) throws SQLException {
        String sql = """
                INSERT INTO item_history (item_id, item_type, date, contribution, user_id)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE contribution = VALUES(contribution)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, itemId);
            statement.setString(2, type);
            statement.setString(3, date);
            statement.setInt(4, contribution);
            statement.setString(5, userId);
            statement.executeUpdate();
        }
    }

    private void deleteHistory(Connection connection, String userId, String itemId) throws SQLException {
        String sql = "DELETE FROM item_history WHERE item_id = ? AND user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, itemId);
            statement.setString(2, userId);
            statement.executeUpdate();
        }
    }

    private void adjustHeatmap(Connection connection, String userId, String date, int delta) throws SQLException {
        int current = getHeatmapValue(connection, userId, date);
        int next = current + delta;

        if (next == 0) {
            String deleteSql = "DELETE FROM daily_heatmap WHERE date = ? AND user_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(deleteSql)) {
                statement.setString(1, date);
                statement.setString(2, userId);
                statement.executeUpdate();
            }
            return;
        }

        String upsertSql = """
                INSERT INTO daily_heatmap (date, value, user_id)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE value = VALUES(value)
                """;
        try (PreparedStatement statement = connection.prepareStatement(upsertSql)) {
            statement.setString(1, date);
            statement.setInt(2, next);
            statement.setString(3, userId);
            statement.executeUpdate();
        }
    }

    private int getHeatmapValue(Connection connection, String userId, String date) throws SQLException {
        String sql = "SELECT value FROM daily_heatmap WHERE date = ? AND user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, date);
            statement.setString(2, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("value");
                }
            }
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
        dto.setUserId(resultSet.getString("user_id"));
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
        statement.setString(9, dto.getUserId());
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

    @FunctionalInterface
    private interface SqlWork<T> {
        T run(Connection connection) throws SQLException;
    }

    private <T> T inTransaction(SqlWork<T> work) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (Exception ex) {
                connection.rollback();
                if (ex instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new IllegalStateException("Falha na transação do banco de dados", ex);
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Falha na transação do banco de dados", ex);
        }
    }
}