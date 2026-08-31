#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$ROOT_DIR/.env}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "[ERRO] arquivo env nao encontrado."
  exit 1
fi

cd "$ROOT_DIR"

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

docker compose --env-file "$ENV_FILE" -f docker-compose.app.yml -f docker-compose.build.yml build api
docker compose --env-file "$ENV_FILE" -f docker-compose.app.yml -f docker-compose.build.yml up -d --no-deps --force-recreate api

echo "=== App ==="
echo "Task Manager API: http://localhost:${API_PORT:-8080}"
