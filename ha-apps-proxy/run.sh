#!/bin/sh
set -e

echo "[INFO] Reading add-on configuration..."
SERVER_HOST=$(jq -r '.server_host' /data/options.json)
FRONTEND_PORT=$(jq -r '.frontend_port' /data/options.json)
BACKEND_PORT=$(jq -r '.backend_port' /data/options.json)

echo "[INFO] Configuration: server_host=${SERVER_HOST}, frontend_port=${FRONTEND_PORT}, backend_port=${BACKEND_PORT}"

echo "[INFO] Discovering ingress path from Supervisor API..."
INGRESS_PATH=""
if ADDON_INFO=$(curl -s -H "Authorization: Bearer ${SUPERVISOR_TOKEN}" http://supervisor/addons/self/info); then
    INGRESS_PATH=$(echo "${ADDON_INFO}" | jq -r '.data.ingress_url // empty')
    if [ -n "${INGRESS_PATH}" ]; then
        echo "[INFO] Ingress path: ${INGRESS_PATH}"
    else
        echo "[WARN] Could not extract ingress_url from Supervisor response. sub_filter will be a no-op."
    fi
else
    echo "[WARN] Failed to query Supervisor API. sub_filter will be a no-op."
fi

echo "[INFO] Generating nginx configuration..."
sed \
    -e "s|__SERVER_HOST__|${SERVER_HOST}|g" \
    -e "s|__FRONTEND_PORT__|${FRONTEND_PORT}|g" \
    -e "s|__BACKEND_PORT__|${BACKEND_PORT}|g" \
    -e "s|__INGRESS_PATH__|${INGRESS_PATH}|g" \
    /nginx.conf.template > /tmp/nginx.conf

echo "[INFO] Validating nginx configuration..."
nginx -t -c /tmp/nginx.conf

echo "[INFO] Starting nginx..."
exec nginx -g 'daemon off;' -c /tmp/nginx.conf
