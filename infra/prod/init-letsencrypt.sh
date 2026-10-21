#!/usr/bin/env bash
# First Let's Encrypt certificate for $DOMAIN (run ONCE on the server, from infra/prod/).
# nginx can't start its HTTPS server without a certificate, and certbot needs nginx to answer the
# HTTP-01 challenge — so: a throwaway self-signed certificate → start nginx → real certificate via
# webroot → reload nginx. Renewal afterwards is automatic (the certbot service).
# Usage: ./init-letsencrypt.sh [--staging]   (staging = Let's Encrypt test CA, no rate limits)
set -euo pipefail
cd "$(dirname "$0")"
set -a; source .env; set +a
: "${DOMAIN:?DOMAIN missing in .env}" "${LETSENCRYPT_EMAIL:?LETSENCRYPT_EMAIL missing in .env}"
staging=""
[[ "${1:-}" == "--staging" ]] && staging="--staging"

live="/etc/letsencrypt/live/${DOMAIN}"
echo "### Temporary self-signed certificate for ${DOMAIN}"
docker compose run --rm --entrypoint sh certbot -c "
  mkdir -p '${live}' &&
  openssl req -x509 -nodes -newkey rsa:2048 -days 1 -subj '/CN=localhost' \
    -keyout '${live}/privkey.pem' -out '${live}/fullchain.pem' &&
  cp '${live}/fullchain.pem' '${live}/chain.pem'"

echo "### Starting nginx"
docker compose up -d nginx

echo "### Removing the temporary certificate"
docker compose run --rm --entrypoint sh certbot -c "
  rm -rf '/etc/letsencrypt/live/${DOMAIN}' '/etc/letsencrypt/archive/${DOMAIN}' '/etc/letsencrypt/renewal/${DOMAIN}.conf'"

echo "### Requesting the certificate from Let's Encrypt"
docker compose run --rm --entrypoint certbot certbot certonly --webroot -w /var/www/certbot \
  ${staging} -d "${DOMAIN}" --email "${LETSENCRYPT_EMAIL}" --agree-tos --no-eff-email --rsa-key-size 4096

echo "### Reloading nginx"
docker compose exec nginx nginx -s reload
echo "Done. Renewal is automatic (certbot service + nginx reload every 6 h)."
