#!/bin/sh
# Prepare the private HiveMQ configuration for docker-compose.remote.ghcr.yml on Synology DSM.
set -eu
umask 077

PROJECT_ROOT=$(CDPATH= cd "$(dirname "$0")/.." && pwd)
TEMPLATE_FILE="$PROJECT_ROOT/config/hivemq-remote.env.example"
LOCAL_DIR="$PROJECT_ROOT/docker/local"
REMOTE_ENV="$LOCAL_DIR/hivemq-remote.env"

if [ ! -f "$PROJECT_ROOT/sql/schema.sql" ] || [ ! -f "$TEMPLATE_FILE" ]; then
    echo "Missing schema or HiveMQ remote configuration template." >&2
    exit 1
fi
if ! command -v openssl >/dev/null 2>&1; then
    echo "openssl was not found; it is required to generate the private database password." >&2
    exit 1
fi

mkdir -p "$LOCAL_DIR"
if [ ! -f "$REMOTE_ENV" ]; then
    cp "$TEMPLATE_FILE" "$REMOTE_ENV"
fi

mysql_password=$(sed -n 's/^MYSQL_ROOT_PASSWORD=//p' "$REMOTE_ENV" | tail -n 1)
if [ -z "$mysql_password" ]; then
    mysql_password=$(openssl rand -hex 24)
    temp_file="$REMOTE_ENV.tmp.$$"
    grep -v '^MYSQL_ROOT_PASSWORD=' "$REMOTE_ENV" > "$temp_file" || true
    printf 'MYSQL_ROOT_PASSWORD=%s\n' "$mysql_password" >> "$temp_file"
    mv "$temp_file" "$REMOTE_ENV"
fi
db_password=$(sed -n 's/^DB_PASSWORD=//p' "$REMOTE_ENV" | tail -n 1)
if [ "$db_password" != "$mysql_password" ]; then
    temp_file="$REMOTE_ENV.tmp.$$"
    grep -v '^DB_PASSWORD=' "$REMOTE_ENV" > "$temp_file" || true
    printf 'DB_PASSWORD=%s\n' "$mysql_password" >> "$temp_file"
    mv "$temp_file" "$REMOTE_ENV"
fi
chmod 600 "$REMOTE_ENV"

for key in MQTT_MODE MQTT_BROKER_URL MQTT_USERNAME MQTT_PASSWORD; do
    value=$(sed -n "s/^${key}=//p" "$REMOTE_ENV" | tail -n 1)
    case "$value" in
        ""|*'<'*|*'>'*)
            echo "Set $key in the private $REMOTE_ENV file, then run this script again." >&2
            exit 1
            ;;
    esac
done

if ! grep -q '^MQTT_MODE=remote$' "$REMOTE_ENV"; then
    echo "MQTT_MODE must be remote in $REMOTE_ENV." >&2
    exit 1
fi
if ! grep -q '^MQTT_BROKER_URL=ssl://' "$REMOTE_ENV"; then
    echo "MQTT_BROKER_URL must start with ssl:// so TLS certificate validation and SNI use the HiveMQ hostname." >&2
    exit 1
fi

echo "Synology remote MQTT private configuration is ready. HiveMQ credentials remain only in docker/local/hivemq-remote.env."
