# Preparação do Banco de Dados

O TaskFlow usa **exclusivamente o MySQL do sistema** em `127.0.0.1:3306` e cria o schema automaticamente no primeiro acesso. Este guia explica a preparação do banco.

> O TaskFlow **não** utiliza mais uma instância alternativa em `/tmp`. O MySQL do sistema é **obrigatório**; se ele estiver fora do ar, o `run.sh` encerra com um erro orientando a iniciá-lo (ex.: `sudo systemctl start mysql`).

## Arquivos relacionados

| Arquivo | Papel |
|---|---|
| `db/schema.sql` | **Bootstrap** (executado com sudo uma vez) — cria os bancos `taskflow_db` e `taskflow_test` e o usuário da aplicação. Não cria tabelas. |
| `src/main/resources/db/migration/V1__*.sql` | **Migrações Flyway** — criam as tabelas. Aplicadas automaticamente por `util.DatabaseConnection.initialize()` no startup. |
| `docs/database/schema.md` | **Explicativo** — documenta as tabelas. |

> As tabelas são criadas pelas migrações Flyway (`V1__create_tables.sql`) quando a aplicação inicia, então **não é preciso criar tabelas manualmente** em cenário normal. Detalhes em [docs/database/migrations.md](../database/migrations.md).

## Credenciais de conexão

| Variável | Padrão | Descrição |
|---|---|---|
| `MYSQL_HOST` | `127.0.0.1` | Host do MySQL do sistema. |
| `MYSQL_PORT` | `3306` | Porta do MySQL do sistema. |
| `MYSQL_DB` | `taskflow_db` | Nome do banco de dados. |
| `MYSQL_USER` | `taskflow` | Usuário da aplicação. |
| `MYSQL_PASSWORD` | `TaskFlow@2026` | Senha padrão de desenvolvimento. |

Os mesmos valores padrão são usados por `run.sh` e por `util/DatabaseConnection`.

## Preparação (uma única vez)

No Ubuntu/Debian, o `root` do MySQL usa `auth_socket` (login apenas por `sudo mysql`). Aplique o `db/schema.sql` — idempotente — para criar os bancos e o usuário `taskflow`:

```bash
sudo mysql < db/schema.sql
```

O que o arquivo faz:
- cria o banco `taskflow_db` (se não existir);
- cria o banco `taskflow_test` (usado pela suíte de testes);
- cria/garante o usuário `'taskflow'@'localhost'` e `'taskflow'@'127.0.0.1'` com a senha `TaskFlow@2026` (satisfaz a política MEDIUM do MySQL 8.0.46);
- concede privilégios sobre `taskflow_db` e `taskflow_test` ao usuário `taskflow`.

O aplicativo usa apenas o usuário `taskflow` — o `root` do MySQL não é usado pela aplicação.

## Iniciar a aplicação

Com o MySQL do sistema ativo e o usuário `taskflow` criado, basta:

```bash
./run.sh
```

O script:
1. conecta no MySQL do sistema em `127.0.0.1:3306` com `taskflow`/`TaskFlow@2026`;
2. garante que `taskflow_db` exista;
3. inicia a API Java na porta `8080` apontando para esse banco.

Se o MySQL não estiver acessível, o script **encerra com erro** e informa como iniciar o serviço — ele não cria banco alternativo.

Alternativamente, sem o script:

```bash
mvn exec:java
```

Na inicialização a aplicação conecta ao MySQL, garante o banco `taskflow_db` e aplica as migrações Flyway (versionadas, uma única vez), criando as tabelas. Se uma migração falhar, o startup **aborta** com erro.

## Backup

O `run.sh` mantém um dump do banco em `$HOME/.taskflow/taskflow_db.sql.gz` (fora de `/tmp`), usado pelo serviço systemd no encerramento (`run.sh backup`). O backup é tratado **apenas como backup** — ele não é usado para recriar um banco alternativo.

Para gerar o backup manualmente:

```bash
./run.sh backup
```

## Verificar a instalação

Com o MySQL do sistema (porta 3306) e o usuário `taskflow`:

```bash
mysql -h127.0.0.1 -P3306 -utaskflow -p'TaskFlow@2026' -e "USE taskflow_db; SHOW TABLES;"
```

Tabelas esperadas (após a primeira inicialização da aplicação):

```
daily_heatmap
habit
item_history
recurring_task
task
usuario
```

## Migrações

Evolução de schema é feita com **Flyway** (migrações SQL versionadas em `src/main/resources/db/migration/`). A aplicação **nunca** executa `ALTER` ad-hoc no startup nem atribui/exclui dados sem dono automaticamente. Detalhes em [docs/database/migrations.md](../database/migrations.md).