# Schema do Banco de Dados

O schema executável fica em `db/schema.sql` e é aplicado automaticamente no startup da aplicação (ver [setup/database.md](../setup/database.md)). Este documento explica cada tabela.

## user

Usuários da aplicação. O identificador de login é a coluna `user` (e-mail cadastrado no frontend); a senha é guardada como `salt:hash` (SHA-256 com salt — nunca em texto puro).

| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | VARCHAR(64) | Chave primária (UUID encurtado). |
| `user` | VARCHAR(255) | Identificador de login (e-mail). |
| `data` | DATE | Data de criação da conta. |
| `senha` | VARCHAR(128) | Hash da senha (`salt:hash`). |

## task, habit, recurring_task

Tarefas, hábitos e tarefas recorrentes. As três tabelas têm exatamente a mesma estrutura e são diferenciadas apenas pelo nome da tabela (decisão em [decisions.md](../architecture/decisions.md)).

| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | VARCHAR(64) | Chave primária (UUID encurtado). |
| `nome` | VARCHAR(255) | Nome da atividade. |
| `data` | DATE | Data vinculada à atividade. |
| `todos_os_dias` | TINYINT(1) | Se a atividade é diária (1) ou pontual (0). |
| `vezes_ao_dia` | VARCHAR(64) | Quantas vezes por dia a atividade deve ser feita (usado como peso no heatmap). |
| `completed_today` | TINYINT(1) | Se foi concluída hoje. |
| `completion_count` | INT | Acumulador de conclusões. |
| `status` | VARCHAR(64) | `pending`, `completed` ou `failed`. |

## item_history

Histórico de contribuições de cada atividade para o heatmap (normalizado).

| Coluna | Tipo | Descrição |
|---|---|---|
| `item_id` | VARCHAR(64) | ID da atividade. |
| `item_type` | VARCHAR(64) | Tipo: `task`, `habit` ou `recurring`. |
| `date` | DATE | Data da contribuição. |
| `contribution` | INT | Peso somado naquela data (positivo para conclusão, negativo para falha). |

Chave primária composta: `(item_id, item_type, date)`.

## daily_heatmap

Agregação desnormalizada do heatmap por data, para leitura rápida na tela inicial.

| Coluna | Tipo | Descrição |
|---|---|---|
| `date` | DATE | Chave primária. |
| `value` | INT | Soma das contribuições naquela data. |

> O valor `0` ou a ausência da linha significam "sem atividade" no heatmap.

## Consultas úteis

Veja `db/queries.sql` para exemplos de consulta prontos para explorar os dados manualmente.