#!/usr/bin/env bash
#
# Sobe a infraestrutura do TaskFlow:
#   1. MySQL do sistema (porta 3306) se acessível com as credenciais
#      configuradas — preferido, pois é persistente. Nesse caso, encerra e
#      remove a instância isolada em /tmp (que fica redundante).
#   2. Caso contrário, sobe uma instância isolada em /tmp/taskflow-mysql
#      (necessária no Ubuntu por causa do AppArmor, que restringe o mysqld
#      a /tmp e /var/lib/mysql).
#   3. Inicia a API Java na porta 8080 apontando para o MySQL escolhido.
#
# Nota: a instância isolada vive em /tmp e é apagada em reinicializações —
# rode ./run.sh novamente após reiniciar o PC. Para uma solução persistente,
# configure o MySQL do sistema (ver docs/setup/database.md).
#
set -euo pipefail

TASKFLOW_TMP="${TASKFLOW_TMP:-/tmp/taskflow-mysql}"
MYSQL_DATA_DIR="$TASKFLOW_TMP/data"
MYSQL_SOCKET="$TASKFLOW_TMP/mysql.sock"
MYSQL_LOG="$TASKFLOW_TMP/mysqld.log"
API_PORT="${API_PORT:-8080}"
API_LOG="$TASKFLOW_TMP/api.log"
BACKUP_DIR="${TASKFLOW_BACKUP_DIR:-$HOME/.taskflow}"
BACKUP_FILE="$BACKUP_DIR/taskflow_db.sql.gz"
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Credenciais configuráveis (mesmas usadas por util/DatabaseConnection)
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_DB="${MYSQL_DB:-taskflow_db}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-root}"

MYSQL_CMD=(mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD")

# ------------------------------------------------------- Detecção do MySQL
if "${MYSQL_CMD[@]}" -e "SELECT 1" >/dev/null 2>&1; then
  echo "[mysql] usando MySQL do sistema em $MYSQL_HOST:$MYSQL_PORT (persistente)"
  # Garante que o banco exista (o JDBC também cria via createDatabaseIfNotExist)
  "${MYSQL_CMD[@]}" -e "CREATE DATABASE IF NOT EXISTS \`$MYSQL_DB\`" >/dev/null 2>&1 || true

  # Instância isolada em /tmp agora é redundante — encerra e remove
  if [ -S "$MYSQL_SOCKET" ] && mysqladmin --socket="$MYSQL_SOCKET" -uroot -proot ping >/dev/null 2>&1; then
    echo "[mysql] fazendo backup da instância isolada antes de encerrá-la"
    backup_mysql 127.0.0.1 3307
    echo "[mysql] encerrando instância isolada ($TASKFLOW_TMP)"
    mysqladmin --socket="$MYSQL_SOCKET" -uroot -proot shutdown >/dev/null 2>&1 || true
    sleep 1
    # API antiga apontava para o banco isolado — será reiniciada no banco do sistema
    pkill -f 'app.TaskApplication' 2>/dev/null || true
  fi
  if [ -d "$TASKFLOW_TMP" ] && { [ -S "$MYSQL_SOCKET" ] || [ -d "$MYSQL_DATA_DIR" ]; }; then
    rm -rf "$TASKFLOW_TMP"
    echo "[mysql] instância isolada removida ($TASKFLOW_TMP)"
  fi
else
  echo "[mysql] sem acesso a $MYSQL_HOST:$MYSQL_PORT — usando instância isolada em /tmp"

  mkdir -p "$TASKFLOW_TMP"
  MYSQL_HOST=127.0.0.1
  MYSQL_PORT=3307
MYSQL_CMD=(mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD")

# ------------------------------------------------------------- Backup/Restore
# Cria um dump do banco em $BACKUP_FILE (fora do /tmp) sempre que houver dados.
backup_mysql() {
  local host="$1" port="$2"
  local cmd=(mysql -h"$host" -P"$port" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD")
  local tables
  tables="$("${cmd[@]}" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$MYSQL_DB'" 2>/dev/null || true)"
  if [ "${tables:-0}" = "0" ]; then
    return 0
  fi

  mkdir -p "$BACKUP_DIR"
  if mysqldump -h"$host" -P"$port" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" \
      --single-transaction "$MYSQL_DB" 2>/dev/null | gzip > "$BACKUP_FILE.tmp"; then
    if [ -s "$BACKUP_FILE.tmp" ]; then
      mv "$BACKUP_FILE.tmp" "$BACKUP_FILE"
      echo "[backup] banco salvo em $BACKUP_FILE"
    else
      rm -f "$BACKUP_FILE.tmp"
    fi
  else
    rm -f "$BACKUP_FILE.tmp"
  fi
}

# Se o banco está vazio e existe um backup, restaura os dados (sobrevive a reinícios).
restore_mysql() {
  local host="$1" port="$2"
  local cmd=(mysql -h"$host" -P"$port" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD")
  [ -f "$BACKUP_FILE" ] || return 0

  local has_users
  has_users="$("${cmd[@]}" -N -e "SELECT COUNT(*) FROM \`$MYSQL_DB\`.usuario" 2>/dev/null || true)"
  [ "${has_users:-0}" != "0" ] && return 0

  echo "[backup] banco vazio — restaurando $BACKUP_FILE"
  gunzip -c "$BACKUP_FILE" | "${cmd[@]}" "$MYSQL_DB" >/dev/null 2>&1 || echo "[backup] falha ao restaurar"
}

# Modo "backup": usado pelo systemd no encerramento (ExecStop).
if [ "${1:-}" = "backup" ]; then
  if mysqladmin --socket="$MYSQL_SOCKET" -uroot -proot ping >/dev/null 2>&1; then
    backup_mysql 127.0.0.1 3307
  else
    backup_mysql "$MYSQL_HOST" "$MYSQL_PORT"
  fi
  exit 0
fi

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
  fi

  if ! "${MYSQL_CMD[@]}" -e "SELECT 1" >/dev/null 2>&1; then
    echo "[mysql] falha ao conectar na instância isolada" >&2
    exit 1
  fi
  echo "[mysql] pronto (porta $MYSQL_PORT)"

  # Restaura dados de um reinício anterior (o backup vive fora do /tmp)
  restore_mysql 127.0.0.1 3307
  # Mantém o backup sempre atualizado
  backup_mysql 127.0.0.1 3307

  echo
  echo "[aviso] os dados estão em /tmp e seriam apagados a cada reinício do PC."
  echo "[aviso] Para persistência real, rode UMA VEZ com a senha do sudo:"
  echo "        cd $BASE_DIR && sudo mysql < db/schema.sql"
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
  MYSQL_HOST="$MYSQL_HOST" MYSQL_PORT="$MYSQL_PORT" MYSQL_DB="$MYSQL_DB" \
    MYSQL_USER="$MYSQL_USER" MYSQL_PASSWORD="$MYSQL_PASSWORD" \
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