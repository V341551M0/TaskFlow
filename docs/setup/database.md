# Preparação do Banco de Dados

O TaskFlow usa **MySQL** e cria o schema automaticamente no primeiro acesso. Este guia explica a preparação do banco nas duas formas suportadas.

## Arquivos relacionados

| Arquivo | Papel |
|---|---|
| `db/schema.sql` | **Executável** — cria as tabelas (`CREATE TABLE IF NOT EXISTS`). |
| `db/data.sql` | Dados iniciais (atualmente vazio; o seed de exemplo é feito em código). |
| `db/queries.sql` | Consultas de apoio para exploração manual. |
| `docs/database/schema.md` | **Explicativo** — documenta as tabelas. |

> O schema é aplicado automaticamente pelo `util.DatabaseConnection.initialize()` quando a aplicação inicia, então **não é preciso executar `schema.sql` manualmente** em cenário normal.

## Opção A — MySQL do sistema (3306)

### 1. Garantir um usuário com senha

No Ubuntu/Debian, o `root` do MySQL usa `auth_socket` (login apenas por `sudo mysql`), o que impede a conexão JDBC com `root/root`. Crie/ajuste um usuário com senha:

```sql
-- acesse com: sudo mysql
CREATE USER IF NOT EXISTS 'root'@'127.0.0.1' IDENTIFIED BY 'root';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'127.0.0.1' WITH GRANT OPTION;
FLUSH PRIVILEGES;
```

### 2. Configurar credenciais

O app lê variáveis de ambiente (ver [development.md](development.md)):

```bash
export MYSQL_HOST=127.0.0.1 MYSQL_PORT=3306 MYSQL_USER=root MYSQL_PASSWORD=root
```

Com os valores padrão (`localhost:3306`, `root`/`root`), basta garantir que o banco `taskflow_db` seja criado — o próprio JDBC cria com `createDatabaseIfNotExist=true`.

### 3. Iniciar a aplicação

```bash
mvn exec:java
```

Na inicialização a aplicação:
1. conecta ao MySQL;
2. cria o banco `taskflow_db` (se não existir);
3. executa `db/schema.sql` (idempotente);
4. insere dados de exemplo se as tabelas de itens estiverem vazias.

## Opção B — Instância isolada (script `run.sh`)

Para não depender do MySQL do sistema (usuário/senha, AppArmor), o `run.sh` cria uma instância própria:

```bash
./run.sh
```

- Datadir e dados: `/tmp/taskflow-mysql/data`
- Porta: `3307`
- Usuário/senha: `root` / `root`
- Banco: `taskflow_db`

> Por usar `/tmp`, os dados são **efêmeros** (apagados em reinicialização). Rode `./run.sh` novamente para recriar. Para dados persistentes, use a Opção A.

## Verificar a instalação

```bash
mysql -h127.0.0.1 -P3307 -uroot -proot -e "USE taskflow_db; SHOW TABLES;"
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