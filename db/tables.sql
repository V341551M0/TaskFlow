/*
    Estrutura das tabelas (MySQL)
    Executado pelo aplicativo no startup (DatabaseConnection.initialize()).
    Idempotente: CREATE TABLE IF NOT EXISTS.
*/

CREATE TABLE IF NOT EXISTS usuario (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    user VARCHAR(255) NOT NULL,
    data DATE,
    senha VARCHAR(512),
    UNIQUE KEY uk_usuario_email (email),
    UNIQUE KEY uk_usuario_user (user)
);

CREATE TABLE IF NOT EXISTS task (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    data DATE,
    todos_os_dias TINYINT(1) NOT NULL DEFAULT 0,
    vezes_ao_dia VARCHAR(64) NOT NULL DEFAULT '1',
    completed_today TINYINT(1) NOT NULL DEFAULT 0,
    completion_count INT NOT NULL DEFAULT 0,
    status VARCHAR(64) NOT NULL DEFAULT 'pending',
    user_id VARCHAR(64) NOT NULL,
    KEY idx_task_user (user_id),
    CONSTRAINT fk_task_user FOREIGN KEY (user_id) REFERENCES usuario (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS habit (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    data DATE,
    todos_os_dias TINYINT(1) NOT NULL DEFAULT 0,
    vezes_ao_dia VARCHAR(64) NOT NULL DEFAULT '1',
    completed_today TINYINT(1) NOT NULL DEFAULT 0,
    completion_count INT NOT NULL DEFAULT 0,
    status VARCHAR(64) NOT NULL DEFAULT 'pending',
    user_id VARCHAR(64) NOT NULL,
    KEY idx_habit_user (user_id),
    CONSTRAINT fk_habit_user FOREIGN KEY (user_id) REFERENCES usuario (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS recurring_task (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    data DATE,
    todos_os_dias TINYINT(1) NOT NULL DEFAULT 0,
    vezes_ao_dia VARCHAR(64) NOT NULL DEFAULT '1',
    completed_today TINYINT(1) NOT NULL DEFAULT 0,
    completion_count INT NOT NULL DEFAULT 0,
    status VARCHAR(64) NOT NULL DEFAULT 'pending',
    user_id VARCHAR(64) NOT NULL,
    KEY idx_recurring_task_user (user_id),
    CONSTRAINT fk_recurring_task_user FOREIGN KEY (user_id) REFERENCES usuario (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS item_history (
    item_id VARCHAR(64) NOT NULL,
    item_type VARCHAR(64) NOT NULL,
    date DATE NOT NULL,
    contribution INT NOT NULL DEFAULT 0,
    user_id VARCHAR(64) NOT NULL,
    PRIMARY KEY (item_id, item_type, date),
    KEY idx_item_history_user (user_id),
    CONSTRAINT fk_item_history_user FOREIGN KEY (user_id) REFERENCES usuario (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS daily_heatmap (
    date DATE NOT NULL,
    value INT NOT NULL DEFAULT 0,
    user_id VARCHAR(64) NOT NULL,
    PRIMARY KEY (user_id, date),
    KEY idx_heatmap_date (date),
    CONSTRAINT fk_heatmap_user FOREIGN KEY (user_id) REFERENCES usuario (id) ON DELETE CASCADE
);