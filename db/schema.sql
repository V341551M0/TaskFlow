/*
    Bootstrap do MySQL (executado com privilégios de root, ex.:
        sudo mysql < db/schema.sql
    Cria os bancos e o usuário de aplicação. Idempotente.
    As tabelas são criadas pelo aplicativo no startup (db/tables.sql).
*/

CREATE DATABASE IF NOT EXISTS taskflow_db;
CREATE DATABASE IF NOT EXISTS taskflow_test;

CREATE USER IF NOT EXISTS 'taskflow'@'localhost' IDENTIFIED BY 'taskflow';
CREATE USER IF NOT EXISTS 'taskflow'@'127.0.0.1' IDENTIFIED BY 'taskflow';
GRANT ALL PRIVILEGES ON taskflow_db.* TO 'taskflow'@'localhost';
GRANT ALL PRIVILEGES ON taskflow_db.* TO 'taskflow'@'127.0.0.1';
GRANT ALL PRIVILEGES ON taskflow_test.* TO 'taskflow'@'localhost';
GRANT ALL PRIVILEGES ON taskflow_test.* TO 'taskflow'@'127.0.0.1';
FLUSH PRIVILEGES;

/*
    Conta legada usada pela instância isolada em /tmp (run.sh).
    Mantida para compatibilidade.
*/
CREATE USER IF NOT EXISTS 'root'@'127.0.0.1' IDENTIFIED BY 'root';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'127.0.0.1' WITH GRANT OPTION;
FLUSH PRIVILEGES;