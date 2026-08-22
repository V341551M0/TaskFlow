# Relacionamentos

## Visão geral

```
usuario
  │  (user_id com FOREIGN KEY, ON DELETE CASCADE)
  ├── task
  ├── habit
  ├── recurring_task
  ├── item_history
  └── daily_heatmap

task ───────────── item_history ───────────── daily_heatmap
habit ───────────── item_history ───────────── daily_heatmap
recurring_task ──── item_history ───────────── daily_heatmap
```

## Relacionamento lógico: atividades → histórico

- Cada linha de `item_history` referencia uma atividade através de `item_id` + `item_type`.
- `item_type` diz qual tabela consultar: `task`, `habit` ou `recurring_task`.
- Ao concluir (ou falhar) uma atividade, o sistema:
  1. grava/atualiza a contribuição em `item_history`;
  2. atualiza o agregado em `daily_heatmap` (aplica `contribution` na data).

```
task (id=abc, vezes_ao_dia=2) ──conclui em 2026-08-16──►
  item_history (abc, task, 2026-08-16, +2)
  daily_heatmap (2026-08-16, +2)
```

## Usuários e atividades

Cada atividade pertence a um usuário: as tabelas `task`, `habit`, `recurring_task`, `item_history` e `daily_heatmap` possuem `user_id` com `FOREIGN KEY` para `usuario` (`ON DELETE CASCADE`). Todas as consultas do repositório filtram por `user_id` do usuário autenticado (`AuthContext`), garantindo **isolamento total entre contas** — um usuário nunca vê nem altera atividades de outro.

## Foreign keys

O schema (migração `V1__create_tables.sql`) declara `FOREIGN KEY` com `ON DELETE CASCADE` de todas as tabelas para `usuario` (e de `item_history`/`daily_heatmap` para as atividades via `item_id` lógico).

## Regras de manutenção do heatmap

- **Concluir** → soma `vezes_ao_dia` na data.
- **Falhar** → subtrai `vezes_ao_dia` na data.
- **Apagar** uma atividade → remove seu histórico e reverte as contribuições do heatmap.
- Se o valor de uma data chega a `0` ou menos, a linha é removida de `daily_heatmap`.