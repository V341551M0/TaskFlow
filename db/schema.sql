/*
    Estrutura do Banco De Dados
*/

CREATE TABLE task (
    id INT NOT NULL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    data DATE,
    descricao TEXT
);

CREATE TABLE recurring_task (
    id INT NOT NULL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    date DATE,
    descricao TEXT
);

CREATE TABLE habit (
    id INT NOT NULL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    date DATE,
    descricao TEXT
)