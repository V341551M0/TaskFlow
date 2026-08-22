package util;

/**
 * Suporte compartilhado para testes de integração com banco.
 * <p>
 * Os testes rodam contra o banco dedicado {@code taskflow_test} (definido no
 * surefire) e limpam as tabelas antes de cada teste. Em CI, a recomendação é
 * substituir esse banco compartilhado por uma instância MySQL via Testcontainers
 * (ver docs/database/migrations.md) — este helper concentra a limpeza para que
 * o único ponto de mutação global fique num só lugar.
 */
public final class TestDbSupport {
    private TestDbSupport() {
    }

    public static void clearTables() {
        try (var connection = DatabaseConnection.getConnection();
             var statement = connection.createStatement()) {
            statement.execute("DELETE FROM item_history");
            statement.execute("DELETE FROM daily_heatmap");
            statement.execute("DELETE FROM task");
            statement.execute("DELETE FROM habit");
            statement.execute("DELETE FROM recurring_task");
            statement.execute("DELETE FROM usuario");
        } catch (Exception ex) {
            throw new IllegalStateException("Não foi possível limpar as tabelas de teste", ex);
        }
    }
}