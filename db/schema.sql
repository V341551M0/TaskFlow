/*
    Estrutura do Banco De Dados
*/

CREATE TABLE IF NOT EXISTS task (
    id TEXT NOT NULL PRIMARY KEY,
    nome TEXT NOT NULL,
    data TEXT,
    todos_os_dias INTEGER NOT NULL DEFAULT 0,
    vezes_ao_dia TEXT NOT NULL DEFAULT '1',
    completed_today INTEGER NOT NULL DEFAULT 0,
    completion_count INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'pending'
);

CREATE TABLE IF NOT EXISTS habit (
    id TEXT NOT NULL PRIMARY KEY,
    nome TEXT NOT NULL,
    data TEXT,
    todos_os_dias INTEGER NOT NULL DEFAULT 0,
    vezes_ao_dia TEXT NOT NULL DEFAULT '1',
    completed_today INTEGER NOT NULL DEFAULT 0,
    completion_count INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'pending'
);

CREATE TABLE IF NOT EXISTS recurring_task (
    id TEXT NOT NULL PRIMARY KEY,
    nome TEXT NOT NULL,
    data TEXT,
    todos_os_dias INTEGER NOT NULL DEFAULT 0,
    vezes_ao_dia TEXT NOT NULL DEFAULT '1',
    completed_today INTEGER NOT NULL DEFAULT 0,
    completion_count INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'pending'
);

CREATE TABLE IF NOT EXISTS item_history (
    item_id TEXT NOT NULL,
    item_type TEXT NOT NULL,
    date TEXT NOT NULL,
    contribution INTEGER NOT NULL,
    PRIMARY KEY (item_id, date)
);

CREATE TABLE IF NOT EXISTS daily_heatmap (
    date TEXT NOT NULL PRIMARY KEY,
    value INTEGER NOT NULL DEFAULT 0
);
