#!/usr/bin/env bash
# Self-signed certificate for the local rehearsal (CN/SAN localhost). Not for the server.
set -euo pipefail
cd "$(dirname "$0")"
dir=letsencrypt/live/localhost
mkdir -p "$dir"
openssl req -x509 -nodes -newkey rsa:2048 -days 7 -subj "/CN=localhost" \
  -addext "subjectAltName=DNS:localhost" -keyout "$dir/privkey.pem" -out "$dir/fullchain.pem" 2>/dev/null
cp "$dir/fullchain.pem" "$dir/chain.pem"
chmod 644 "$dir"/*.pem
echo "$dir ready"
