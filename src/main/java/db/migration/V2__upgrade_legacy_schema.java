package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Atualiza o schema criado antes da adoção de Flyway e do isolamento por usuário.
 *
 * <p>O schema antigo não tinha {@code user_id}. Caso existam atividades e exatamente
 * um usuário, os registros são atribuídos a ele. Com mais de um usuário, a autoria
 * é ambígua e a migração falha antes de modificar dados, exigindo uma decisão manual.
 * Isso evita perda de dados ou uma atribuição silenciosa ao usuário errado.</p>
 */
public class V2__upgrade_legacy_schema extends BaseJavaMigration {
    private static final List<String> ITEM_TABLES = List.of("task", "habit", "recurring_task", "item_history");

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        validateLegacyOwnership(connection);

        widenUserColumns(connection);
        ensureUniqueIndexes(connection);

        for (String table : ITEM_TABLES) {
            ensureUserColumn(connection, table);
        }
        ensureHeatmapUserColumn(connection);

        String ownerId = legacyOwnerId(connection);
        if (ownerId != null) {
            assignLegacyRows(connection, ownerId);
        }

        requireUserId(connection, ITEM_TABLES);
        requireUserId(connection, List.of("daily_heatmap"));
        ensureIndexesAndForeignKeys(connection);
    }

    private void validateLegacyOwnership(Connection connection) throws SQLException {
        if (hasColumn(connection, "task", "user_id")) {
            return;
        }

        long activities = countRows(connection, "task")
                + countRows(connection, "habit")
                + countRows(connection, "recurring_task")
                + countRows(connection, "item_history")
                + countRows(connection, "daily_heatmap");
        long users = countRows(connection, "usuario");

        if (activities > 0 && users != 1) {
            throw new SQLException("O schema legado possui " + activities + " registro(s) de atividade e " + users
                    + " usuário(s). Não é seguro decidir automaticamente a quem pertencem os dados. "
                    + "Crie uma migração de propriedade explícita antes de iniciar a aplicação.");
        }

        ensureNoDuplicates(connection, "email");
        ensureNoDuplicates(connection, "user");
    }

    private void widenUserColumns(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE usuario MODIFY email VARCHAR(255) NOT NULL");
            statement.execute("ALTER TABLE usuario MODIFY senha VARCHAR(512)");
        }
    }

    private void ensureUniqueIndexes(Connection connection) throws SQLException {
        if (!hasIndex(connection, "usuario", "uk_usuario_email")) {
            execute(connection, "ALTER TABLE usuario ADD UNIQUE KEY uk_usuario_email (email)");
        }
        if (!hasIndex(connection, "usuario", "uk_usuario_user")) {
            execute(connection, "ALTER TABLE usuario ADD UNIQUE KEY uk_usuario_user (`user`)");
        }
    }

    private void ensureUserColumn(Connection connection, String table) throws SQLException {
        if (!hasColumn(connection, table, "user_id")) {
            execute(connection, "ALTER TABLE " + table + " ADD COLUMN user_id VARCHAR(64) NULL");
        }
    }

    private void ensureHeatmapUserColumn(Connection connection) throws SQLException {
        if (!hasColumn(connection, "daily_heatmap", "user_id")) {
            execute(connection, "ALTER TABLE daily_heatmap ADD COLUMN user_id VARCHAR(64) NULL");
        }
    }

    private String legacyOwnerId(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM usuario ORDER BY data, id LIMIT 1");
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getString(1) : null;
        }
    }

    private void assignLegacyRows(Connection connection, String ownerId) throws SQLException {
        for (String table : ITEM_TABLES) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE " + table + " SET user_id = ? WHERE user_id IS NULL OR user_id = ''")) {
                statement.setString(1, ownerId);
                statement.executeUpdate();
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE daily_heatmap SET user_id = ? WHERE user_id IS NULL OR user_id = ''")) {
            statement.setString(1, ownerId);
            statement.executeUpdate();
        }
    }

    private void requireUserId(Connection connection, List<String> tables) throws SQLException {
        for (String table : tables) {
            if (countRowsWithMissingUser(connection, table) > 0) {
                throw new SQLException("A tabela " + table + " ainda possui registros sem usuário. Migração interrompida sem remover dados.");
            }
            if (isNullable(connection, table, "user_id")) {
                execute(connection, "ALTER TABLE " + table + " MODIFY user_id VARCHAR(64) NOT NULL");
            }
        }
    }

    private void ensureIndexesAndForeignKeys(Connection connection) throws SQLException {
        for (String table : ITEM_TABLES) {
            String index = "idx_" + table + "_user";
            String foreignKey = "fk_" + table + "_user";
            if (!hasIndex(connection, table, index)) {
                execute(connection, "ALTER TABLE " + table + " ADD INDEX " + index + " (user_id)");
            }
            if (!hasForeignKey(connection, table, foreignKey)) {
                execute(connection, "ALTER TABLE " + table + " ADD CONSTRAINT " + foreignKey
                        + " FOREIGN KEY (user_id) REFERENCES usuario (id) ON DELETE CASCADE");
            }
        }

        if (!hasPrimaryKeyStartingWith(connection, "daily_heatmap", "user_id")) {
            execute(connection, "ALTER TABLE daily_heatmap DROP PRIMARY KEY, ADD PRIMARY KEY (user_id, date)");
        }
        if (!hasIndex(connection, "daily_heatmap", "idx_heatmap_date")) {
            execute(connection, "ALTER TABLE daily_heatmap ADD INDEX idx_heatmap_date (date)");
        }
        if (!hasForeignKey(connection, "daily_heatmap", "fk_heatmap_user")) {
            execute(connection, "ALTER TABLE daily_heatmap ADD CONSTRAINT fk_heatmap_user"
                    + " FOREIGN KEY (user_id) REFERENCES usuario (id) ON DELETE CASCADE");
        }
    }

    private void ensureNoDuplicates(Connection connection, String column) throws SQLException {
        String sql = "SELECT " + column + " FROM usuario GROUP BY " + column + " HAVING COUNT(*) > 1 LIMIT 1";
        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
            if (resultSet.next()) {
                throw new SQLException("Não é possível criar o índice único: usuario." + column + " possui valores duplicados.");
            }
        }
    }

    private long countRows(Connection connection, String table) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private long countRowsWithMissingUser(Connection connection, String table) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + table + " WHERE user_id IS NULL OR user_id = ''")) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private boolean hasColumn(Connection connection, String table, String column) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    private boolean isNullable(Connection connection, String table, String column) throws SQLException {
        String sql = "SELECT IS_NULLABLE FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Coluna ausente: " + table + "." + column);
                }
                return "YES".equals(resultSet.getString(1));
            }
        }
    }

    private boolean hasIndex(Connection connection, String table, String index) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.STATISTICS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            statement.setString(2, index);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    private boolean hasForeignKey(Connection connection, String table, String foreignKey) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS "
                + "WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = ? "
                + "AND CONSTRAINT_NAME = ? AND CONSTRAINT_TYPE = 'FOREIGN KEY'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            statement.setString(2, foreignKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    private boolean hasPrimaryKeyStartingWith(Connection connection, String table, String column) throws SQLException {
        String sql = "SELECT COLUMN_NAME FROM information_schema.STATISTICS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? "
                + "AND INDEX_NAME = 'PRIMARY' AND SEQ_IN_INDEX = 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && column.equals(resultSet.getString(1));
            }
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
