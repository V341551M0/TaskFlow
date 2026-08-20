/*
    Bootstrap do MySQL (executado com privilégios de root, ex.:
        sudo mysql < db/schema.sql
    Cria os bancos e o usuário de aplicação. Idempotente.
    As tabelas são criadas pelo aplicativo no startup via Flyway
    (src/main/resources/db/migration), não por este arquivo.

    Arquitetura atual: uma única instância persistente — o MySQL do sistema
    em 127.0.0.1:3306. Não há instância alternativa em /tmp e não existem
    contas legadas (ex.: 'root'@'127.0.0.1').
*/

CREATE DATABASE IF NOT EXISTS taskflow_db;
CREATE DATABASE IF NOT EXISTS taskflow_test;

-- Usuário da aplicação. A senha "TaskFlow@2026" satisfaz a política MEDIUM
-- do MySQL 8.0.46 (8+ caracteres, maiúscula, número e caractere especial).
-- Não é a senha de usuários da aplicação (essas ficam em PasswordUtil).
CREATE USER IF NOT EXISTS 'taskflow'@'localhost' IDENTIFIED BY 'TaskFlow@2026';
CREATE USER IF NOT EXISTS 'taskflow'@'127.0.0.1' IDENTIFIED BY 'TaskFlow@2026';

-- Garante a senha mesmo se o usuário já existir com outra anteriormente.
ALTER USER 'taskflow'@'localhost' IDENTIFIED BY 'TaskFlow@2026';
ALTER USER 'taskflow'@'127.0.0.1' IDENTIFIED BY 'TaskFlow@2026';

-- Privilégios escopados SOMENTE aos bancos da aplicação — nunca em *.*.
GRANT ALL PRIVILEGES ON taskflow_db.* TO 'taskflow'@'localhost';
GRANT ALL PRIVILEGES ON taskflow_db.* TO 'taskflow'@'127.0.0.1';

GRANT ALL PRIVILEGES ON taskflow_test.* TO 'taskflow'@'localhost';
GRANT ALL PRIVILEGES ON taskflow_test.* TO 'taskflow'@'127.0.0.1';

FLUSH PRIVILEGES;