# Migrações de Banco de Dados

## Como o schema é aplicado hoje

O projeto usa **Flyway** (9.x) com migrações SQL versionadas. Na inicialização, `util.DatabaseConnection.initialize()` executa `Flyway.migrate()` sobre `taskflow_db` (e `taskflow_test` na suíte de testes).

Se a migração falhar (erro de SQL ou checksum divergente), a aplicação **aborta a inicialização** com uma mensagem clara — nunca tenta "consertar" por conta própria.

```
src/main/resources/db/migration/
├── V1__create_tables.sql   ← schema inicial (criação das 6 tabelas)
├── V2__upgrade_legacy_schema.java ← adapta o schema anterior ao Flyway
└── V3__*.sql               ← próximas mudanças (adições/alterações)
```

> O `db/schema.sql` é apenas o **bootstrap** (cria os bancos e o usuário `taskflow`, executado com `sudo` uma única vez). Ele **não cria tabelas** — as tabelas vêm das migrações Flyway.

## Compatibilidade com o schema anterior

Versões antigas do TaskFlow criavam as tabelas diretamente, sem a tabela
`flyway_schema_history` e sem a coluna `user_id`. Para evitar que a API pare de
iniciar após a atualização, o Flyway registra esse schema como baseline na versão
1 e executa a migração Java `V2__upgrade_legacy_schema`.

- Se não houver atividades, a atualização é automática.
- Se houver atividades e exatamente um usuário, os dados legados são atribuídos a
  esse usuário.
- Se houver atividades com zero ou mais de um usuário, a migração falha antes de
  alterar dados, pois a autoria é ambígua. Faça backup e crie uma migração de
  propriedade explícita para esse caso.

## Regras de evolução

1. **Nunca altere uma migração já aplicada** — crie uma nova `V<N+1>__descricao.sql`. Alterar um arquivo já executado gera erro de checksum no startup (abort seguro).
2. **Uma migração = uma mudança atômica**, que é aplicada dentro de uma transação do Flyway.
3. **Migração de dados com dono ausente é proibida**: a aplicação **não** atribui automaticamente registros órfãos a um usuário (ex.: "primeiro usuário") nem **exclui** dados sem dono. Se houver dados legados sem `user_id`, um passo explícito e auditável de triagem (arquivamento ou atribuição manual por um admin) deve ocorrer **antes** da migração de estrutura, em uma migração própria.
4. **Compatibilidade controlada**: schemas legados conhecidos recebem baseline na
   versão 1 e são atualizados pela V2. Schemas com dados cuja propriedade não pode
   ser determinada continuam bloqueando o startup, sem exclusão ou atribuição
   silenciosa de registros.

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

Se a base de produção ainda tiver o schema antigo sem histórico Flyway, a V2 faz a
adaptação apenas quando a propriedade dos dados é inequívoca. Em qualquer cenário
ambíguo, o startup aborta para exigir uma decisão explícita e auditável.

## Dados iniciais

Não há seed de dados de exemplo: as tabelas de itens (task, habit, recurring_task) começam **vazias** e são preenchidas apenas quando o usuário cria atividades na aplicação.
