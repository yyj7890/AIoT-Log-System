#!/bin/sh
# Prepare the private Docker files required by docker-compose.yml on Synology DSM.
# Run this once through SSH from the project root before creating the DSM project.
set -eu
umask 077

PROJECT_ROOT=$(CDPATH= cd "$(dirname "$0")/.." && pwd)
ENV_FILE="$PROJECT_ROOT/.env"
LOCAL_DIR="$PROJECT_ROOT/docker/local"

for required_file in sql/schema.sql; do
    if [ ! -f "$PROJECT_ROOT/$required_file" ]; then
        echo "Missing required project file: $PROJECT_ROOT/$required_file" >&2
        echo "Upload the complete project directory before running this script." >&2
        exit 1
    fi
done

if ! command -v docker >/dev/null 2>&1; then
    echo "Docker was not found. Run this on the Synology NAS where Container Manager is installed." >&2
    exit 1
fi

if ! command -v openssl >/dev/null 2>&1; then
    echo "openssl was not found; it is required to generate private credentials." >&2
    exit 1
fi

mkdir -p "$LOCAL_DIR"
touch "$ENV_FILE"
chmod 600 "$ENV_FILE"

# A failed first Compose attempt can create empty directories where bind-mounted
# files should be. Remove only those empty placeholders before creating files.
for private_file in mosquitto-lan.conf mosquitto-acl.conf mosquitto-passwords mqtt-credentials.env mqtt-credentials.properties; do
    if [ -d "$LOCAL_DIR/$private_file" ]; then
        if ! rmdir "$LOCAL_DIR/$private_file"; then
            echo "Expected a file but found a non-empty directory: $LOCAL_DIR/$private_file" >&2
            echo "Move its contents elsewhere, remove that directory, then run this script again." >&2
            exit 1
        fi
    fi
done

get_env_value() {
    key=$1
    [ -f "$ENV_FILE" ] || return 0
    sed -n "s/^${key}=//p" "$ENV_FILE" | tail -n 1
}

set_env_value() {
    key=$1
    value=$2
    temp_file="$ENV_FILE.tmp.$$"
    if [ -f "$ENV_FILE" ]; then
        grep -v "^${key}=" "$ENV_FILE" > "$temp_file" || true
    else
        : > "$temp_file"
    fi
    printf '%s=%s\n' "$key" "$value" >> "$temp_file"
    mv "$temp_file" "$ENV_FILE"
    chmod 600 "$ENV_FILE"
}

ensure_secret() {
    key=$1
    current=$(get_env_value "$key")
    if [ -z "$current" ]; then
        current=$(openssl rand -hex 24)
        set_env_value "$key" "$current"
    fi
    printf '%s' "$current"
}

mysql_password=$(ensure_secret MYSQL_ROOT_PASSWORD)
mqtt_username=$(get_env_value MQTT_USERNAME)
if [ -z "$mqtt_username" ]; then
    mqtt_username=aiot-device
    set_env_value MQTT_USERNAME "$mqtt_username"
fi
mqtt_password=$(ensure_secret MQTT_PASSWORD)

for setting in MYSQL_PORT=3306 MQTT_PORT=1883 MQTT_DISCOVERY_PORT=19830 BACKEND_PORT=8080 WEB_PORT=80; do
    key=${setting%%=*}
    default=${setting#*=}
    if [ -z "$(get_env_value "$key")" ]; then
        set_env_value "$key" "$default"
    fi
done

lan_ip=$(ip route get 1 2>/dev/null | awk '{ for (i = 1; i <= NF; i++) if ($i == "src") { print $(i + 1); exit } }' || true)
if [ -z "$lan_ip" ]; then
    lan_ip=$(hostname -I 2>/dev/null | awk '{ print $1 }' || true)
fi
case "$lan_ip" in
    ''|127.*|169.254.*)
        set_env_value MQTT_DISCOVERY_ENABLED false
        set_env_value MQTT_DISCOVERY_BROKER_HOST ""
        ;;
    *)
        set_env_value MQTT_DISCOVERY_ENABLED true
        set_env_value MQTT_DISCOVERY_BROKER_HOST "$lan_ip"
        ;;
esac

cat > "$LOCAL_DIR/mosquitto-lan.conf" <<'EOF'
persistence true
persistence_location /mosquitto/data/
log_dest stdout
listener 1883 0.0.0.0
allow_anonymous false
password_file /mosquitto/config/mosquitto-passwords
acl_file /mosquitto/config/mosquitto-acl.conf
EOF

cat > "$LOCAL_DIR/mosquitto-acl.conf" <<EOF
# Generated Synology Docker ACL. Keep this file private.
user $mqtt_username
topic readwrite aiot/device/+/report
topic readwrite aiot/device/+/log
EOF

cat > "$LOCAL_DIR/mqtt-credentials.env" <<EOF
# Generated Synology Docker credential. Keep this file private.
MQTT_USERNAME=$mqtt_username
MQTT_PASSWORD=$mqtt_password
EOF

cat > "$LOCAL_DIR/mqtt-credentials.properties" <<EOF
# Generated Synology Docker credential. Keep this file private.
mqtt.username=$mqtt_username
mqtt.password=$mqtt_password
EOF

if [ ! -f "$LOCAL_DIR/mosquitto-passwords" ]; then
    docker run --rm -v "$LOCAL_DIR:/work" eclipse-mosquitto:2 \
        mosquitto_passwd -b -c /work/mosquitto-passwords "$mqtt_username" "$mqtt_password" >/dev/null
fi

# The official Mosquitto image runs as UID/GID 1883. Keep the password hash
# private while making it readable by that container user on DSM bind mounts.
if ! chown 1883:1883 "$LOCAL_DIR/mosquitto-passwords" "$LOCAL_DIR/mosquitto-acl.conf"; then
    echo "Could not assign Mosquitto security files to UID/GID 1883. Run this script as root on DSM." >&2
    exit 1
fi
chmod 600 "$ENV_FILE" "$LOCAL_DIR/mosquitto-passwords" "$LOCAL_DIR/mosquitto-acl.conf" "$LOCAL_DIR/mqtt-credentials.env" "$LOCAL_DIR/mqtt-credentials.properties"
chmod 644 "$LOCAL_DIR/mosquitto-lan.conf"

# Keep shellcheck from treating values intentionally kept only in private files as unused.
: "$mysql_password"
echo "Synology Docker private configuration is ready. Create or start the Container Manager project now."
