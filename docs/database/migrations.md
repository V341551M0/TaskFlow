# Migrações de Banco de Dados

## Como o schema é aplicado hoje

O projeto usa **Flyway** (9.x) com migrações SQL versionadas. Na inicialização, `util.DatabaseConnection.initialize()` executa `Flyway.migrate()` sobre `taskflow_db` (e `taskflow_test` na suíte de testes).

Se a migração falhar (erro de SQL, checksum divergente, ou schema não vazio sem histórico), a aplicação **aborta a inicialização** com uma mensagem clara — nunca tenta "consertar" por conta própria.

```
src/main/resources/db/migration/
├── V1__create_tables.sql   ← schema inicial (criação das 6 tabelas)
└── V2__*.sql               ← próximas mudanças (adições/alterações)
```

> O `db/schema.sql` é apenas o **bootstrap** (cria os bancos e o usuário `taskflow`, executado com `sudo` uma única vez). Ele **não cria tabelas** — as tabelas vêm das migrações Flyway.

## Regras de evolução

1. **Nunca altere uma migração já aplicada** — crie uma nova `V<N+1>__descricao.sql`. Alterar um arquivo já executado gera erro de checksum no startup (abort seguro).
2. **Uma migração = uma mudança atômica**, que é aplicada dentro de uma transação do Flyway.
3. **Migração de dados com dono ausente é proibida**: a aplicação **não** atribui automaticamente registros órfãos a um usuário (ex.: "primeiro usuário") nem **exclui** dados sem dono. Se houver dados legados sem `user_id`, um passo explícito e auditável de triagem (arquivamento ou atribuição manual por um admin) deve ocorrer **antes** da migração de estrutura, em uma migração própria.
4. **Abortar em vez de contornar**: `baselineOnMigrate=false`. Se o banco tiver tabelas mas nenhum histórico Flyway, o startup falha pedindo a migração consciente em vez de adivinhar a linha de base.

## Adicionar uma mudança nova

1. Crie `src/main/resources/db/migration/V<N+1>__descricao.sql` com a mudança (`CREATE TABLE`, `ALTER TABLE`, `CREATE INDEX`, etc.).
2. Reinicie a aplicação — o Flyway aplica automaticamente e registra no histórico.
3. Documente a mudança em [schema.md](schema.md) e [relationships.md](relationships.md) quando alterar a estrutura.

## Validar migrações antes de produção

Antes de aplicar em produção, valide a sequência de migrações em um banco descartável no MySQL do sistema em `127.0.0.1:3306` (ex.: `taskflow_test`) com um dump restaurado do banco de produção:

```bash
# 1. restaure o dump de produção em um banco descartável do MySQL do sistema
gunzip -c backup_producao.sql.gz | \
  mysql -h127.0.0.1 -P3306 -utaskflow -p'TaskFlow@2026' taskflow_test

# 2. rode a aplicação apontando para esse banco
MYSQL_HOST=127.0.0.1 MYSQL_PORT=3306 MYSQL_DB=taskflow_test \
  MYSQL_USER=taskflow MYSQL_PASSWORD='TaskFlow@2026' mvn exec:java

# 3. a aplicação deve iniciar sem erro; verifique o histórico:
mysql -h127.0.0.1 -P3306 -utaskflow -p'TaskFlow@2026' \
  -e "SELECT version, description, success FROM taskflow_test.flyway_schema_history;"
```

Com `baselineOnMigrate=false`, se a base de produção ainda tiver o schema antigo sem histórico Flyway, o startup **aborta** — nesse caso a migração exige uma decisão explícita e auditável (nunca automática).

## Dados iniciais

Não há seed de dados de exemplo: as tabelas de itens (task, habit, recurring_task) começam **vazias** e são preenchidas apenas quando o usuário cria atividades na aplicação.