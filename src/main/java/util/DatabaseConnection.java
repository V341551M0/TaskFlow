package util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseConnection {
    private static final String HOST = getEnvOrDefault("MYSQL_HOST", "localhost");
    private static final String PORT = getEnvOrDefault("MYSQL_PORT", "3306");
    private static final String DATABASE = getEnvOrDefault("MYSQL_DB", "taskflow_db");
    private static final String USER = getEnvOrDefault("MYSQL_USER", "taskflow");
    private static final String PASSWORD = getEnvOrDefault("MYSQL_PASSWORD", "taskflow");

    private static final String JDBC_URL = String.format(
            "jdbc:mysql://%s:%s/%s?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
            HOST, PORT, DATABASE
    );

    private DatabaseConnection() {
    }

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            System.err.println("Driver MySQL JDBC não encontrado no classpath: " + ex.getMessage());
        }
    }

    public static void initialize() {
        try (Connection connection = getConnection()) {
            try (Statement statement = connection.createStatement()) {
                for (String sql : loadTableStatements()) {
                    statement.execute(sql);
                }
            }
            migrate(connection);
        } catch (SQLException | IOException ex) {
            throw new IllegalStateException("Não foi possível inicializar o banco de dados MySQL", ex);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
    }

    private static String[] loadTableStatements() throws IOException {
        Path tablesPath = Path.of("db/tables.sql");
        if (!Files.exists(tablesPath)) {
            throw new IOException("Arquivo de tabelas não encontrado: " + tablesPath);
        }

        String tables = Files.readString(tablesPath, StandardCharsets.UTF_8);
        tables = tables.replaceAll("/\\*[\\s\\S]*?\\*/", " ");
        return java.util.Arrays.stream(tables.split(";"))
                .map(String::trim)
                .filter(statement -> !statement.isBlank())
                .toArray(String[]::new);
    }

    private static void migrate(Connection connection) {
        for (String table : new String[]{"task", "habit", "recurring_task", "item_history"}) {
            ensureUserColumn(connection, table);
        }
        ensureHeatmapUserColumn(connection);
        ensureUniqueUserIdentifiers(connection);
    }

    private static boolean hasColumn(Connection connection, String table, String column) {
        String sql = "SELECT COUNT(*) FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        } catch (SQLException ex) {
            return false;
        }
    }

    private static void ensureUserColumn(Connection connection, String table) {
        if (hasColumn(connection, table, "user_id")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN user_id VARCHAR(64) NULL");
            statement.execute("ALTER TABLE " + table + " ADD INDEX idx_" + table + "_user (user_id)");
            backfillUserId(statement, table);
            try {
                statement.execute("ALTER TABLE " + table
                        + " ADD CONSTRAINT fk_" + table + "_user"
                        + " FOREIGN KEY (user_id) REFERENCES usuario (id) ON DELETE CASCADE");
            } catch (SQLException ex) {
                System.err.println("[migração] não foi possível criar FK em " + table + ": " + ex.getMessage());
            }
        } catch (SQLException ex) {
            System.err.println("[migração] não foi possível adicionar user_id em " + table + ": " + ex.getMessage());
        }
    }

    private static void ensureHeatmapUserColumn(Connection connection) {
        boolean hasUserColumn = hasColumn(connection, "daily_heatmap", "user_id");
        boolean pkStartsWithUserId = hasPrimaryKeyColumn(connection, "daily_heatmap", "user_id", 1);

        if (hasUserColumn && pkStartsWithUserId) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            if (!hasUserColumn) {
                statement.execute("ALTER TABLE daily_heatmap ADD COLUMN user_id VARCHAR(64) NULL");
            }

            if (!pkStartsWithUserId) {
                backfillUserId(statement, "daily_heatmap");
                statement.execute("ALTER TABLE daily_heatmap DROP PRIMARY KEY");
                statement.execute("ALTER TABLE daily_heatmap ADD PRIMARY KEY (user_id, date)");
                statement.execute("ALTER TABLE daily_heatmap ADD INDEX idx_heatmap_date (date)");
            }

            try {
                statement.execute("ALTER TABLE daily_heatmap ADD CONSTRAINT fk_heatmap_user"
                        + " FOREIGN KEY (user_id) REFERENCES usuario (id) ON DELETE CASCADE");
            } catch (SQLException ex) {
                System.err.println("[migração] não foi possível criar FK em daily_heatmap: " + ex.getMessage());
            }
        } catch (SQLException ex) {
            System.err.println("[migração] não foi possível ajustar daily_heatmap: " + ex.getMessage());
        }
    }

    private static boolean hasPrimaryKeyColumn(Connection connection, String table, String column, int position) {
        String sql = "SELECT COLUMN_NAME FROM information_schema.STATISTICS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = 'PRIMARY' AND SEQ_IN_INDEX = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            statement.setInt(2, position);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && column.equals(resultSet.getString("COLUMN_NAME"));
            }
        } catch (SQLException ex) {
            return false;
        }
    }

    private static void backfillUserId(Statement statement, String table) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery("SELECT id FROM usuario ORDER BY data LIMIT 1")) {
            if (resultSet.next()) {
                String firstUserId = resultSet.getString("id");
                statement.execute("UPDATE " + table + " SET user_id = '" + firstUserId
                        + "' WHERE user_id IS NULL OR user_id = ''");
            } else {
                statement.execute("DELETE FROM " + table + " WHERE user_id IS NULL OR user_id = ''");
            }
        }
    }

    private static void ensureUniqueUserIdentifiers(Connection connection) {
        if (hasIndex(connection, "usuario", "uk_usuario_email") && hasIndex(connection, "usuario", "uk_usuario_user")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE usuario ADD UNIQUE KEY uk_usuario_email (email)");
        } catch (SQLException ex) {
            System.err.println("[migração] não foi possível criar UNIQUE em usuario.email: " + ex.getMessage());
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE usuario ADD UNIQUE KEY uk_usuario_user (user)");
        } catch (SQLException ex) {
            System.err.println("[migração] não foi possível criar UNIQUE em usuario.user: " + ex.getMessage());
        }
    }

    private static boolean hasIndex(Connection connection, String table, String indexName) {
        String sql = "SELECT COUNT(*) FROM information_schema.STATISTICS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            statement.setString(2, indexName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        } catch (SQLException ex) {
            return false;
        }
    }

    private static String getEnvOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }
}