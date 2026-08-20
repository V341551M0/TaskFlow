#!/usr/bin/env bash
#
# Sobe a infraestrutura do TaskFlow usando exclusivamente o MySQL do sistema:
#   1. Conecta no MySQL persistente em 127.0.0.1:3306 com o usuário "taskflow".
#   2. Garante que o banco taskflow_db exista.
#   3. Inicia a API Java na porta 8080 apontando para esse banco.
#
# Se o MySQL do sistema não estiver acessível, o script encerra com erro —
# NÃO cria instância alternativa em /tmp (os dados devem viver apenas no
# MySQL persistente).
#
# Modo "backup": cria um dump do banco em $BACKUP_FILE (usado pelo systemd
# no encerramento via ExecStop).
#
set -euo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$BASE_DIR/.env"

# Carrega .env (ignorado pelo Git) se existir — permite sobrescrever as
# credenciais MYSQL_* e a JWT_SECRET por ambiente, sem valores em código.
if [ -f "$ENV_FILE" ]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

# Credenciais do MySQL persistente do sistema (mesmas usadas por util/DatabaseConnection)
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_DB="${MYSQL_DB:-taskflow_db}"
MYSQL_USER="${MYSQL_USER:-taskflow}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-TaskFlow@2026}"

API_PORT="${API_PORT:-8080}"
API_LOG="${API_LOG:-/tmp/taskflow-api.log}"
CP_FILE="${CP_FILE:-/tmp/taskflow-classpath.txt}"
BACKUP_DIR="${TASKFLOW_BACKUP_DIR:-$HOME/.taskflow}"
BACKUP_FILE="$BACKUP_DIR/taskflow_db.sql.gz"

# JWT_SECRET é obrigatória para a aplicação (não há default no código). Se não
# estiver definida, gera uma e persiste no .env para que os tokens sobrevivam a
# reinicializações locais.
if [ -z "${JWT_SECRET:-}" ]; then
  JWT_SECRET="$(openssl rand -hex 32 2>/dev/null || head -c 64 /dev/urandom | tr -dc 'a-f0-9')"
  printf 'JWT_SECRET=%s\n' "$JWT_SECRET" >> "$ENV_FILE"
  echo "[api] JWT_SECRET gerada e salva em .env (mantenha em segredo; não versione)"
fi

MYSQL_CMD=(mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD")

# ------------------------------------------------------------- Backup
# Cria um dump do banco persistente em $BACKUP_FILE (fora do /tmp).
# Tratado apenas como backup — nunca usado para criar um banco alternativo.
backup_mysql() {
  local tables
  tables="$("${MYSQL_CMD[@]}" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$MYSQL_DB'" 2>/dev/null || true)"
  if [ "${tables:-0}" = "0" ]; then
    return 0
  fi

  mkdir -p "$BACKUP_DIR"
  if mysqldump -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" \
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

# Modo "backup": usado pelo systemd no encerramento (ExecStop).
# Dump exclusivamente do MySQL persistente em 127.0.0.1:3306 com taskflow/TaskFlow@2026.
if [ "${1:-}" = "backup" ]; then
  backup_mysql
  exit 0
fi

# ------------------------------------------------------- Verificação do MySQL
if ! "${MYSQL_CMD[@]}" -e "SELECT 1" >/dev/null 2>&1; then
  echo "[mysql] ERRO: MySQL do sistema não está acessível em $MYSQL_HOST:$MYSQL_PORT com o usuário '$MYSQL_USER'." >&2
  echo "[mysql] O TaskFlow usa exclusivamente o MySQL persistente do sistema — não há fallback em /tmp." >&2
  echo "[mysql] Inicie o serviço MySQL, por exemplo:" >&2
  echo "        sudo systemctl start mysql" >&2
  echo "[mysql] E certifique-se de que o usuário '$MYSQL_USER' existe (execute uma vez):" >&2
  echo "        cd $BASE_DIR && sudo mysql < db/schema.sql" >&2
  exit 1
fi

echo "[mysql] usando MySQL do sistema em $MYSQL_HOST:$MYSQL_PORT (persistente)"
# Garante que o banco exista (o JDBC também cria via createDatabaseIfNotExist)
"${MYSQL_CMD[@]}" -e "CREATE DATABASE IF NOT EXISTS \`$MYSQL_DB\`" >/dev/null 2>&1 || true

# Mantém o backup sempre atualizado
backup_mysql

# ------------------------------------------------------------------ API
if curl -s -m 2 -o /dev/null "http://localhost:$API_PORT/api/tasks"; then
  echo "[api] já rodando em http://localhost:$API_PORT"
else
  if [ ! -f "$BASE_DIR/target/classes/app/TaskApplication.class" ]; then
    echo "[api] compilando projeto..."
    mvn -q -f "$BASE_DIR/pom.xml" compile
  fi

  mvn -q -f "$BASE_DIR/pom.xml" dependency:build-classpath \
    -Dmdep.outputFile="$CP_FILE" 2>/dev/null || true

  echo "[api] iniciando TaskApplication na porta $API_PORT"
  cd "$BASE_DIR"
  CP="target/classes:$(cat "$CP_FILE" 2>/dev/null || true)"
  MYSQL_HOST="$MYSQL_HOST" MYSQL_PORT="$MYSQL_PORT" MYSQL_DB="$MYSQL_DB" \
    MYSQL_USER="$MYSQL_USER" MYSQL_PASSWORD="$MYSQL_PASSWORD" JWT_SECRET="$JWT_SECRET" \
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
echo "MySQL    : $MYSQL_HOST:$MYSQL_PORT (banco $MYSQL_DB, usuário $MYSQL_USER)"