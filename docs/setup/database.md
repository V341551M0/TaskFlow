# Preparação do Banco de Dados

O TaskFlow usa **MySQL** e cria o schema automaticamente no primeiro acesso. Este guia explica a preparação do banco nas duas formas suportadas e como o `run.sh` escolhe entre elas.

## Arquivos relacionados

| Arquivo | Papel |
|---|---|
| `db/schema.sql` | **Executável** — cria as tabelas (`CREATE TABLE IF NOT EXISTS`). |
| `docs/database/schema.md` | **Explicativo** — documenta as tabelas. |

> O schema é aplicado automaticamente pelo `util.DatabaseConnection.initialize()` quando a aplicação inicia, então **não é preciso executar `schema.sql` manualmente** em cenário normal.

## Como o `run.sh` escolhe o MySQL

O `./run.sh` verifica, nesta ordem:

1. **MySQL do sistema** (`127.0.0.1:3306`, padrão) — se conseguir conectar com as credenciais configuradas (`MYSQL_USER`/`MYSQL_PASSWORD`, padrão `root`/`root`), **usa esse banco** (persistente). É o cenário recomendado.
2. **Instância isolada** — sem acesso ao do sistema, cria/sobe uma instância própria em `/tmp/taskflow-mysql` na porta `3307` e aponta a API para ela.

As variáveis `MYSQL_HOST`/`MYSQL_PORT`/`MYSQL_DB`/`MYSQL_USER`/`MYSQL_PASSWORD` controlam a conexão (padrões: `127.0.0.1:3306`, `taskflow_db`, `root`/`root`).

## Opção A — MySQL do sistema (3306) — recomendada

### 1. Garantir um usuário com senha

No Ubuntu/Debian, o `root` do MySQL usa `auth_socket` (login apenas por `sudo mysql`), o que impede a conexão JDBC com `root/root`. Crie/ajuste um usuário com senha (uma única vez):

```bash
sudo mysql -e "CREATE USER IF NOT EXISTS 'root'@'127.0.0.1' IDENTIFIED BY 'root'; GRANT ALL PRIVILEGES ON *.* TO 'root'@'127.0.0.1' WITH GRANT OPTION; FLUSH PRIVILEGES;"
```

Equivalente em SQL (via `sudo mysql`):

```sql
CREATE USER IF NOT EXISTS 'root'@'127.0.0.1' IDENTIFIED BY 'root';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'127.0.0.1' WITH GRANT OPTION;
FLUSH PRIVILEGES;
```

### 2. Iniciar a aplicação

Com os valores padrão (`127.0.0.1:3306`, `root`/`root`), basta:

```bash
./run.sh
```

O `run.sh` detecta o MySQL do sistema e usa essa conexão. Alternativamente, sem o script:

```bash
mvn exec:java
```

Na inicialização a aplicação:
1. conecta ao MySQL;
2. cria o banco `taskflow_db` (se não existir);
3. executa `db/schema.sql` (idempotente);
4. insere dados de exemplo se as tabelas de itens estiverem vazias.

## Opção B — Instância isolada (fallback do `run.sh`)

Sem acesso ao MySQL do sistema (sem usuário com senha, AppArmor, etc.), o `run.sh` sobe uma instância própria:

```bash
./run.sh
```

- Datadir e dados: `/tmp/taskflow-mysql/data`
- Porta: `3307`
- Usuário/senha: `root` / `root`
- Banco: `taskflow_db`

> Por usar `/tmp`, os dados são **efêmeros** (apagados em reinicialização). Rode `./run.sh` novamente para recriar. Para dados persistentes, configure a Opção A.

## Verificar a instalação

Com a instância isolada (porta 3307):

```bash
mysql -h127.0.0.1 -P3307 -uroot -proot -e "USE taskflow_db; SHOW TABLES;"
```

Com o MySQL do sistema (porta 3306):

```bash
mysql -h127.0.0.1 -P3306 -uroot -proot -e "USE taskflow_db; SHOW TABLES;"
```

Tabelas esperadas:

```
daily_heatmap
habit
item_history
recurring_task
task
user
```

## Migrações

Para alterar tabelas existentes use `ALTER TABLE` (o `schema.sql` não altera tabelas já criadas). Detalhes em [docs/database/migrations.md](../database/migrations.md).