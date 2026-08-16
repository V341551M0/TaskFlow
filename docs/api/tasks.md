# Endpoints de Tarefas, Hábitos e Recorrentes

Documentação dos endpoints de itens (tarefas, hábitos, tarefas recorrentes), ações de status, exclusão e dashboards.

> Base URL: `http://localhost:8080`. Todos os endpoints aceitam GET, POST ou OPTIONS (CORS).

---

## Listar itens

### `GET /api/tasks` · `GET /api/habits` · `GET /api/recurring-tasks`

Retorna a lista de itens do tipo correspondente.

**Response — 200 OK**

```json
[
  {
    "id": "a6da0830",
    "name": "Estudar Java",
    "date": "2026-08-06",
    "allDays": false,
    "frequencyPerDay": "2",
    "type": "task",
    "completedToday": false,
    "completionCount": 0,
    "status": "pending"
  }
]
```

---

## Criar item

### `POST /api/tasks` · `POST /api/habits` · `POST /api/recurring-tasks`

Cria um item do tipo correspondente.

**Request — `Content-Type: application/json`**

```json
{
  "nome": "Estudar Java",
  "data": "2026-08-16",
  "todosOsDias": "true",
  "vezesAoDia": "2"
}
```

| Campo | Obrigatório | Descrição |
|---|---|---|
| `nome` | sim | Nome da atividade. |
| `data` | não | Data (padrão: hoje). |
| `todosOsDias` | não | `"true"`/`"false"` (padrão: `"false"`). |
| `vezesAoDia` | não | Peso no heatmap (padrão: `"1"`). |

**Response — 201 Created**

```json
{
  "id": "a6da0830",
  "name": "Estudar Java",
  "date": "2026-08-16",
  "allDays": true,
  "frequencyPerDay": "2",
  "type": "task",
  "completedToday": false,
  "completionCount": 0,
  "status": "pending"
}
```

---

## Marcar status (concluir/falhar)

### `POST /api/complete`

Altera o status de um item. Sem o campo `status`, o item é alternado para **concluído**.

**Request**

```json
{
  "id": "a6da0830",
  "type": "task",
  "date": "2026-08-16",
  "status": "completed"
}
```

| Campo | Descrição |
|---|---|
| `id` | ID do item. |
| `type` | `task`, `habit` ou `recurring`. |
| `date` | Data da conclusão/falha (padrão: data do item ou hoje). |
| `status` | `completed`, `failed` ou `pending` (opcional; sem ele, alterna para concluído). |

**Response — 200 OK**: item atualizado (mesma estrutura da listagem).

**Erros**

| Código | Situação |
|---|---|
| `400` | Item já finalizado (concluído/falha) e não pode mudar de status. |
| `404` | Item não encontrado. |

---

## Excluir item

### `POST /api/delete`

Remove o item e reverte suas contribuições no heatmap.

**Request**

```json
{
  "id": "a6da0830",
  "type": "task"
}
```

**Response — 200 OK**: item removido.

**Erros:** `404` quando o item não existe.

---

## Dashboard

### `GET /api/dashboard`

Retorna tudo que a tela inicial precisa em uma única chamada.

**Response — 200 OK**

```json
{
  "tasks": [ "..." ],
  "habits": [ "..." ],
  "recurringTasks": [ "..." ],
  "heatmap": {
    "2026-08-16": 4,
    "2026-08-15": 2
  }
}
```

---

## Heatmap

### `GET /api/heatmap`

Retorna apenas o mapa de calor (`date → value`).

**Response — 200 OK**

```json
{
  "2026-08-16": 4
}
```

---

## Notas

- **CORS:** todos os endpoints respondem com `Access-Control-Allow-Origin: *` e respondem `204` para `OPTIONS`.
- **Formato dos campos:** as requisições aceitam `nome`/`data`/`todosOsDias`/`vezesAoDia`; as respostas devolvem `name`/`date`/`allDays`/`frequencyPerDay`/`type`/`completedToday`/`completionCount`/`status`.
- **IDs:** gerados como UUID encurtado (8 caracteres).