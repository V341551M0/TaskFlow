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
    private static final String DB_PATH = "db/taskflow.db";
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_PATH;

    private DatabaseConnection() {
    }

    public static void initialize() {
        try {
            Files.createDirectories(Path.of("db"));
            try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
                for (String sql : loadSchemaStatements()) {
                    statement.execute(sql);
                }
            }
        } catch (SQLException | IOException ex) {
            throw new IllegalStateException("Não foi possível inicializar o banco de dados", ex);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL);
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
}
