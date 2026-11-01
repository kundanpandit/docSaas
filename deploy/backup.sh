#!/usr/bin/env bash
# Nightly MySQL backup (run by cron from setup-server.sh). Keeps 14 days of compressed dumps in deploy/backups/.
# Restore:  gunzip -c backups/docsaas-YYYY-MM-DD.sql.gz | docker compose -f docker-compose.prod.yml exec -T mysql \
#             sh -c 'mysql -u root -p"$MYSQL_ROOT_PASSWORD" docsaas_db'
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p backups
FILE="backups/docsaas-$(date +%F).sql.gz"

docker compose -f docker-compose.prod.yml --env-file .env exec -T mysql \
  sh -c 'mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" --single-transaction --quick --routines --no-tablespaces docsaas_db' \
  | gzip -9 > "$FILE.tmp"

# Refuse to keep an empty/broken dump
if [[ $(gunzip -c "$FILE.tmp" | head -c 2000 | grep -c "MySQL dump") -eq 0 ]]; then
  echo "$(date '+%F %T') backup FAILED (dump looks empty)"; rm -f "$FILE.tmp"; exit 1
fi
mv "$FILE.tmp" "$FILE" && chmod 600 "$FILE"
find backups -name 'docsaas-*.sql.gz' -mtime +14 -delete
echo "$(date '+%F %T') backup ok: $FILE ($(du -h "$FILE" | cut -f1))"
