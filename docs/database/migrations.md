# Migrações de Banco de Dados

## Como o schema é aplicado hoje

O projeto **não usa Flyway nem Liquibase**. O schema é versionado em `db/schema.sql` e executado automaticamente toda vez que a aplicação inicia, através de `util.DatabaseConnection.initialize()`.

Como as instruções usam `CREATE TABLE IF NOT EXISTS`, o arquivo é **idempotente**: rodar várias vezes não gera erro se as tabelas já existirem.

```
db/
├── schema.sql    ← executável (aplicado no startup)
├── data.sql      ← dados iniciais (por enquanto vazio; o seed é feito pelo TaskRepository)
└── queries.sql   ← consultas de apoio para exploração manual
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
ALTER TABLE `user` ADD COLUMN email VARCHAR(255) NULL;

-- exemplo: criar índice
CREATE INDEX idx_user_user ON `user` (`user`);
```

Em ambiente de desenvolvimento, você pode aplicar o `ALTER` manualmente no MySQL. O arquivo `db/queries.sql` é um bom lugar para registrar esses comandos de evolução.

## Recomendação futura (Flyway/Liquibase)

Se o schema começar a evoluir com frequência, adote uma ferramenta de migração versionada:

- **Flyway**: arquivos `V1__init.sql`, `V2__add_user_id.sql`, ... com histórico controlado.
- **Liquibase**: changelogs em XML/YAML/SQL.

Isso garante que todos os ambientes (local, CI, produção) apliquem a mesma sequência de mudanças sem conflito.

## Semeadura de dados

O seed de dados de exemplo não fica no `data.sql` (que está vazio). Ele é feito em código por `TaskRepository.seedDataIfEmpty()`, que insere exemplos apenas quando as tabelas de itens estão vazias. Para ajustar os dados iniciais, edite esse método ou preencha `db/data.sql` e execute-o manualmente.