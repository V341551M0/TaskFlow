package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.flywaydb.core.Flyway;

public final class DatabaseConnection {
    private static final String HOST = getEnvOrDefault("MYSQL_HOST", "127.0.0.1");
    private static final String PORT = getEnvOrDefault("MYSQL_PORT", "3306");
    private static final String DATABASE = getEnvOrDefault("MYSQL_DB", "taskflow_db");
    private static final String USER = getEnvOrDefault("MYSQL_USER", "taskflow");
    private static final String PASSWORD = getEnvOrDefault("MYSQL_PASSWORD", "TaskFlow@2026");

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

    /**
     * Aplica as migrações versionadas (Flyway) em src/main/resources/db/migration.
     * Falhas de migração abortam a inicialização — nunca aplica ALTERs ad-hoc
     * nem atribui/exclui dados sem dono automaticamente.
     */
    public static void initialize() {
        try {
            Flyway.configure()
                    .dataSource(JDBC_URL, USER, PASSWORD)
                    .baselineOnMigrate(false)
                    .load()
                    .migrate();
        } catch (Exception ex) {
            throw new IllegalStateException("Não foi possível aplicar as migrações do banco de dados", ex);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
    }

    private static String getEnvOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }
}