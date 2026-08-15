/*
    Estrutura do Banco De Dados (MySQL)
*/

CREATE TABLE IF NOT EXISTS task (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    data DATE,
    todos_os_dias TINYINT(1) NOT NULL DEFAULT 0,
    vezes_ao_dia VARCHAR(64) NOT NULL DEFAULT '1',
    completed_today TINYINT(1) NOT NULL DEFAULT 0,
    completion_count INT NOT NULL DEFAULT 0,
    status VARCHAR(64) NOT NULL DEFAULT 'pending'
);

CREATE TABLE IF NOT EXISTS habit (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    data DATE,
    todos_os_dias TINYINT(1) NOT NULL DEFAULT 0,
    vezes_ao_dia VARCHAR(64) NOT NULL DEFAULT '1',
    completed_today TINYINT(1) NOT NULL DEFAULT 0,
    completion_count INT NOT NULL DEFAULT 0,
    status VARCHAR(64) NOT NULL DEFAULT 'pending'
);

CREATE TABLE IF NOT EXISTS recurring_task (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    data DATE,
    todos_os_dias TINYINT(1) NOT NULL DEFAULT 0,
    vezes_ao_dia VARCHAR(64) NOT NULL DEFAULT '1',
    completed_today TINYINT(1) NOT NULL DEFAULT 0,
    completion_count INT NOT NULL DEFAULT 0,
    status VARCHAR(64) NOT NULL DEFAULT 'pending'
);

CREATE TABLE IF NOT EXISTS `user` (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    `user` VARCHAR(255) NOT NULL,
    data DATE,
    senha VARCHAR(128)
);

CREATE TABLE IF NOT EXISTS item_history (
    item_id VARCHAR(64) NOT NULL,
    item_type VARCHAR(64) NOT NULL,
    date DATE NOT NULL,
    contribution INT NOT NULL DEFAULT 0,
    PRIMARY KEY (item_id, item_type, date)
);

CREATE TABLE IF NOT EXISTS daily_heatmap (
    date DATE NOT NULL PRIMARY KEY,
    value INT NOT NULL DEFAULT 0
);

