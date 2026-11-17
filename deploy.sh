#!/usr/bin/env bash
# Faz o deploy da versão atual do repositório e recria os containers alterados.
set -euo pipefail

cd "$(dirname "$0")"

compose() {
    docker compose -f docker-compose.prod.yml "$@"
}

if [ ! -f .env ]; then
    echo "Arquivo .env nao encontrado. Copie .env.example e preencha os valores." >&2
    exit 1
fi

git pull --ff-only

compose build app
compose up -d
docker image prune -f > /dev/null

compose ps
