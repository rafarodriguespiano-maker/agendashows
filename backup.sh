#!/usr/bin/env bash
# Gera um dump compactado do banco e remove backups mais antigos que a retenção.
set -euo pipefail

cd "$(dirname "$0")"

BACKUP_DIR="${BACKUP_DIR:-$HOME/backups/agendashows}"
RETENCAO_DIAS="${RETENCAO_DIAS:-7}"

mkdir -p "$BACKUP_DIR"

arquivo="$BACKUP_DIR/agendashows-$(date +%Y%m%d-%H%M%S).sql.gz"

docker compose -f docker-compose.prod.yml exec -T mysql sh -c \
    'exec mysqldump --single-transaction --quick --routines --triggers -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' \
    | gzip > "$arquivo"

if [ ! -s "$arquivo" ]; then
    echo "Falha ao gerar o backup: arquivo vazio." >&2
    rm -f "$arquivo"
    exit 1
fi

find "$BACKUP_DIR" -type f -name 'agendashows-*.sql.gz' -mtime +"$RETENCAO_DIAS" -delete

echo "Backup gerado: $arquivo"
