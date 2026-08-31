#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$ROOT_DIR/.env}"

if [[ ! -f "$ENV_FILE" ]]; then
    echo "[ERRO] arquivo env nao encontrado."
    exit 1
fi

if [[ "${CONFIRM_RESET:-false}" != "true" ]]; then
  echo "Para resetar o ambiente é necessario flag: CONFIRM_RESET=true"
  exit 1
fi

cd "$ROOT_DIR"

docker compose --env-file "$ENV_FILE" down --volumes --remove-orphans

echo "Reset completo"
