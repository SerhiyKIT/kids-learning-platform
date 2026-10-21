#!/usr/bin/env bash
# Restores a backup made by backup.sh: replaces the database and the MinIO data, then starts the
# app again. Destructive — it is meant for a fresh server or a deliberate rollback.
# Usage: ./restore.sh db-<stamp>.dump[.age] minio-<stamp>.tar.gz[.age]
#   Encrypted (.age) files need AGE_IDENTITY=<path to the age private key file>.
set -euo pipefail
umask 077
db="${1:?usage: restore.sh <db dump> <minio archive>}"
media="${2:?usage: restore.sh <db dump> <minio archive>}"
COMPOSE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$COMPOSE_DIR"
# ENV_FILE / COMPOSE_FILE / COMPOSE_PROJECT_NAME can point the script at another stack (e.g. the
# local rehearsal); on the server the defaults are right.
ENV_FILE="${ENV_FILE:-.env}"
set -a; source "$ENV_FILE"; set +a
compose() { docker compose --env-file "$ENV_FILE" "$@"; }

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
plain() { # $1 = file → path of a decrypted copy
  if [[ "$1" == *.age ]]; then
    age -d -i "${AGE_IDENTITY:?AGE_IDENTITY needed for .age files}" -o "$work/$(basename "${1%.age}")" "$1"
    echo "$work/$(basename "${1%.age}")"
  else
    echo "$1"
  fi
}
db_file="$(plain "$db")"
media_file="$(plain "$media")"

echo "### Stopping the app (the databases stay up)"
compose stop nginx frontend backend
compose up -d --wait postgres minio

echo "### Restoring PostgreSQL"
compose exec -T postgres pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists --no-owner --single-transaction < "$db_file"

echo "### Restoring MinIO data"
compose stop minio
docker run --rm -i -v kidlearn_minio-data:/data alpine:3.20 sh -c 'rm -rf /data/* /data/.[!.]* 2>/dev/null; tar -xzf - -C /data' < "$media_file"

echo "### Starting everything"
compose up -d
echo "Restore finished."
