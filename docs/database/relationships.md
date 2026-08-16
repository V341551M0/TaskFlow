# Relacionamentos

## Visão geral

```
user
  │
  └── (sem vínculo físico por foreign key com os itens hoje)

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

No estado atual, **as atividades não possuem `user_id`**: tarefas, hábitos e tarefas recorrentes são globais ao banco. O `user` é usado apenas para autenticação (login/cadastro).

Isso é uma limitação conhecida e uma evolução natural:

```
User 1 ─────── N Task       (almejado)
User 1 ─────── N Habit
User 1 ─────── N RecurringTask
```

Para implementar, bastaria adicionar `user_id` às três tabelas de itens e filtrar as consultas pelo usuário autenticado.

## Ausência de foreign keys

O schema atual não declara `FOREIGN KEY` (as referências são lógicas). Opções de evolução:

- Adicionar restrições de integridade com `ON DELETE CASCADE` (por exemplo, apagar histórico ao apagar o item).
- Migrar para uma ferramenta de migrações (ver [migrations.md](migrations.md)).

## Regras de manutenção do heatmap

- **Concluir** → soma `vezes_ao_dia` na data.
- **Falhar** → subtrai `vezes_ao_dia` na data.
- **Apagar** uma atividade → remove seu histórico e reverte as contribuições do heatmap.
- Se o valor de uma data chega a `0` ou menos, a linha é removida de `daily_heatmap`.