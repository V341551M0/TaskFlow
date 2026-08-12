package util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseConnection {
    private static final String HOST = getEnvOrDefault("MYSQL_HOST", "localhost");
    private static final String PORT = getEnvOrDefault("MYSQL_PORT", "3306");
    private static final String DATABASE = getEnvOrDefault("MYSQL_DB", "taskflow_db");
    private static final String USER = getEnvOrDefault("MYSQL_USER", "root");
    private static final String PASSWORD = getEnvOrDefault("MYSQL_PASSWORD", "root");

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
        try {
            try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
                for (String sql : loadSchemaStatements()) {
                    statement.execute(sql);
                }
            }
        } catch (SQLException | IOException ex) {
            throw new IllegalStateException("Não foi possível inicializar o banco de dados MySQL", ex);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
    }

    private static String[] loadSchemaStatements() throws IOException {
        Path schemaPath = Path.of("db/schema.sql");
        if (!Files.exists(schemaPath)) {
            throw new IOException("Arquivo de schema não encontrado: " + schemaPath);
        }

        String schema = Files.readString(schemaPath, StandardCharsets.UTF_8);
        schema = schema.replaceAll("/\\*[\\s\\S]*?\\*/", " ");
        return java.util.Arrays.stream(schema.split(";"))
                .map(String::trim)
                .filter(statement -> !statement.isBlank())
                .toArray(String[]::new);
    }

    private static String getEnvOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }
}
