#!/usr/bin/env bash
# Daily backup of the pilot (run by kidlearn-backup.timer, or by hand): a PostgreSQL dump (custom
# format, compressed) and an archive of the MinIO data (voice-line audio), rotated after
# $BACKUP_KEEP_DAYS days. Both contain children's data: the directory and files are root-only
# (700/600). If AGE_RECIPIENT is set, the files are encrypted with `age` right away and no
# plain-text copy is kept — always do that before copying backups off the server (docs/DEPLOY.md).
set -euo pipefail
umask 077

COMPOSE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/kidlearn}"
BACKUP_KEEP_DAYS="${BACKUP_KEEP_DAYS:-14}"
AGE_RECIPIENT="${AGE_RECIPIENT:-}"
stamp="$(date -u +%Y%m%dT%H%M%SZ)"

mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"
cd "$COMPOSE_DIR"
# ENV_FILE / COMPOSE_FILE / COMPOSE_PROJECT_NAME can point the script at another stack (e.g. the
# local rehearsal); on the server the defaults are right.
ENV_FILE="${ENV_FILE:-.env}"
set -a; source "$ENV_FILE"; set +a
compose() { docker compose --env-file "$ENV_FILE" "$@"; }

seal() { # $1 = file: encrypt in place if a recipient is configured
  if [[ -n "$AGE_RECIPIENT" ]]; then
    age -r "$AGE_RECIPIENT" -o "$1.age" "$1" && rm -f "$1"
  fi
}

db="$BACKUP_DIR/db-$stamp.dump"
compose exec -T postgres pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom --compress=9 > "$db"
seal "$db"

media="$BACKUP_DIR/minio-$stamp.tar.gz"
docker run --rm -v kidlearn_minio-data:/data:ro alpine:3.20 tar -czf - -C /data . > "$media"
seal "$media"

find "$BACKUP_DIR" -maxdepth 1 -type f \( -name 'db-*' -o -name 'minio-*' \) -mtime "+$BACKUP_KEEP_DAYS" -delete
echo "Backup $stamp written to $BACKUP_DIR"
