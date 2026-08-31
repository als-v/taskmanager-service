#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$ROOT_DIR/.env}"
SEED_FILE="/seed/seed.sql"

if [[ ! -f "$ENV_FILE" ]]; then
    echo "[ERRO] arquivo env nao encontrado."
    exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

cd "$ROOT_DIR"

echo "Subindo banco.."
until docker compose --env-file "$ENV_FILE" exec -T postgres pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB" >/dev/null 2>&1; do
  sleep 2
done

echo "Seed.."
docker compose --env-file "$ENV_FILE" exec -T postgres psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -f "$SEED_FILE"

echo "Ambiente pronto"
