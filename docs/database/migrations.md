# Migrações de Banco de Dados

## Como o schema é aplicado hoje

O projeto **não usa Flyway nem Liquibase**. O schema é versionado em `db/schema.sql` e executado automaticamente toda vez que a aplicação inicia, através de `util.DatabaseConnection.initialize()`.

Como as instruções usam `CREATE TABLE IF NOT EXISTS`, o arquivo é **idempotente**: rodar várias vezes não gera erro se as tabelas já existirem.

```
db/
└── schema.sql    ← executável (aplicado no startup)
```

> O `db/schema.sql` é a fonte da verdade executável. Este documento (`docs/database/*`) é apenas explicativo.

## Adicionar uma tabela nova

1. Adicione o `CREATE TABLE IF NOT EXISTS` em `db/schema.sql`.
2. Reinicie a aplicação (ou rode o SQL manualmente) para aplicar.
3. Documente a tabela em [schema.md](schema.md).

## Alterar uma tabela existente

`CREATE TABLE IF NOT EXISTS` **não altera** tabelas já existentes. Para mudanças de estrutura use `ALTER TABLE`:

```sql
-- exemplo: adicionar coluna
ALTER TABLE `usuario` ADD COLUMN email VARCHAR(255) NULL;

-- exemplo: criar índice
CREATE INDEX idx_usuario_user ON `usuario` (`user`);
```

Em ambiente de desenvolvimento, você pode aplicar o `ALTER` manualmente no MySQL. Registre os comandos de evolução aqui neste documento para manter o histórico de mudanças.

## Recomendação futura (Flyway/Liquibase)

Se o schema começar a evoluir com frequência, adote uma ferramenta de migração versionada:

- **Flyway**: arquivos `V1__init.sql`, `V2__add_user_id.sql`, ... com histórico controlado.
- **Liquibase**: changelogs em XML/YAML/SQL.

Isso garante que todos os ambientes (local, CI, produção) apliquem a mesma sequência de mudanças sem conflito.

## Dados iniciais

Não há seed de dados de exemplo: as tabelas de itens (task, habit, recurring_task) começam **vazias** e são preenchidas apenas quando o usuário cria atividades na aplicação.