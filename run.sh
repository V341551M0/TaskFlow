#!/usr/bin/env bash
#
# Sobe o MySQL local (instância isolada em /tmp/taskflow-mysql,
# exigência do AppArmor do Ubuntu, que restringe o mysqld) e a API
# Java/MySQL do TaskFlow na porta 8080.
#
# Nota: se o sistema reiniciar, rode ./run.sh novamente.
#
set -euo pipefail

TASKFLOW_TMP="${TASKFLOW_TMP:-/tmp/taskflow-mysql}"
MYSQL_DATA_DIR="$TASKFLOW_TMP/data"
MYSQL_SOCKET="$TASKFLOW_TMP/mysql.sock"
MYSQL_PORT="${MYSQL_PORT:-3307}"
MYSQL_LOG="$TASKFLOW_TMP/mysqld.log"
API_PORT="${API_PORT:-8080}"
API_LOG="$TASKFLOW_TMP/api.log"
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

mkdir -p "$TASKFLOW_TMP"

# ---------------------------------------------------------------- MySQL
if ! mysqladmin --socket="$MYSQL_SOCKET" -uroot -proot ping >/dev/null 2>&1; then
  if [ ! -d "$MYSQL_DATA_DIR" ]; then
    echo "[mysql] inicializando datadir em $MYSQL_DATA_DIR"
    mysqld --no-defaults --initialize-insecure --datadir="$MYSQL_DATA_DIR" --user="$USER"
  fi

  echo "[mysql] iniciando mysqld na porta $MYSQL_PORT"
  setsid nohup mysqld --no-defaults --datadir="$MYSQL_DATA_DIR" \
    --socket="$MYSQL_SOCKET" --port="$MYSQL_PORT" --bind-address=127.0.0.1 \
    --user="$USER" --skip-networking=0 > "$MYSQL_LOG" 2>&1 < /dev/null &

  for _ in $(seq 1 30); do
    if mysqladmin --socket="$MYSQL_SOCKET" -uroot ping >/dev/null 2>&1; then
      break
    fi
    sleep 1
  done

  if ! mysqladmin --socket="$MYSQL_SOCKET" -uroot ping >/dev/null 2>&1; then
    echo "[mysql] falha ao iniciar. Veja $MYSQL_LOG" >&2
    exit 1
  fi

  mysql --socket="$MYSQL_SOCKET" -uroot <<'SQL'
ALTER USER 'root'@'localhost' IDENTIFIED BY 'root';
CREATE USER IF NOT EXISTS 'root'@'127.0.0.1' IDENTIFIED BY 'root';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'127.0.0.1' WITH GRANT OPTION;
FLUSH PRIVILEGES;
CREATE DATABASE IF NOT EXISTS taskflow_db;
SQL
  echo "[mysql] pronto (porta $MYSQL_PORT)"
else
  echo "[mysql] já rodando na porta $MYSQL_PORT"
fi

# ------------------------------------------------------------------ API
if curl -s -m 2 -o /dev/null "http://localhost:$API_PORT/api/tasks"; then
  echo "[api] já rodando em http://localhost:$API_PORT"
else
  if [ ! -f "$BASE_DIR/target/classes/app/TaskApplication.class" ]; then
    echo "[api] compilando projeto..."
    mvn -q -f "$BASE_DIR/pom.xml" compile
  fi

  echo "[api] iniciando TaskApplication na porta $API_PORT"
  cd "$BASE_DIR"
  CP="target/classes:$HOME/.m2/repository/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar"
  MYSQL_HOST=127.0.0.1 MYSQL_PORT="$MYSQL_PORT" MYSQL_USER=root MYSQL_PASSWORD=root \
    setsid nohup java -cp "$CP" app.TaskApplication > "$API_LOG" 2>&1 < /dev/null &

  for _ in $(seq 1 30); do
    if curl -s -m 2 -o /dev/null "http://localhost:$API_PORT/api/tasks"; then
      break
    fi
    sleep 1
  done

  if ! curl -s -m 2 -o /dev/null "http://localhost:$API_PORT/api/tasks"; then
    echo "[api] falha ao iniciar. Veja $API_LOG" >&2
    exit 1
  fi
  echo "[api] pronto (http://localhost:$API_PORT)"
fi

echo
echo "Frontend : http://localhost:5501/web/pages/login.html"
echo "API      : http://localhost:$API_PORT"