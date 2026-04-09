#!/usr/bin/env bash
set -Eeuo pipefail

# One-command deploy script for hotel_service release package.
# Usage:
#   sudo bash deploy.sh /tmp/release-2026-04-06.zip
# Optional:
#   sudo ENV_SOURCE=/tmp/hotel_service.env bash deploy.sh /tmp/release.zip

if [[ "${EUID}" -ne 0 ]]; then
  echo "[ERROR] Please run as root (use sudo)."
  exit 1
fi

if [[ $# -lt 1 ]]; then
  echo "Usage: sudo bash $0 <release-zip-path>"
  exit 1
fi

RELEASE_ZIP="$1"
APP_NAME="hotel_service"
SERVICE_NAME="hotel_service"
APP_USER="hotelsvc"
APP_GROUP="hotelsvc"
APP_DIR="/opt/hotel_service"
WEB_DIR="/var/www/hotel_client"
ENV_DIR="/etc/hotel_service"
ENV_DEST="${ENV_DIR}/hotel_service.env"
ENV_SOURCE="${ENV_SOURCE:-}"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
WORK_DIR="/tmp/${APP_NAME}-deploy-${TIMESTAMP}"

log() {
  echo "[$(date +'%F %T')] $*"
}

cleanup() {
  rm -rf "${WORK_DIR}"
}
trap cleanup EXIT

if [[ ! -f "${RELEASE_ZIP}" ]]; then
  echo "[ERROR] Release package not found: ${RELEASE_ZIP}"
  exit 1
fi

command -v unzip >/dev/null 2>&1 || { echo "[ERROR] unzip is required"; exit 1; }
command -v systemctl >/dev/null 2>&1 || { echo "[ERROR] systemctl is required"; exit 1; }
command -v nginx >/dev/null 2>&1 || { echo "[ERROR] nginx is required"; exit 1; }

log "Creating deploy user and directories"
id -u "${APP_USER}" >/dev/null 2>&1 || useradd --system --no-create-home --shell /usr/sbin/nologin "${APP_USER}"
mkdir -p "${APP_DIR}" "${WEB_DIR}" "${ENV_DIR}" "${WORK_DIR}"

log "Extracting release package: ${RELEASE_ZIP}"
unzip -q "${RELEASE_ZIP}" -d "${WORK_DIR}"

JAR_SRC="${WORK_DIR}/hotel_service.jar"
FRONTEND_SRC="${WORK_DIR}/frontend-dist"

if [[ ! -f "${JAR_SRC}" ]]; then
  echo "[ERROR] Missing hotel_service.jar inside release package"
  exit 1
fi

if [[ ! -d "${FRONTEND_SRC}" ]]; then
  echo "[ERROR] Missing frontend-dist directory inside release package"
  exit 1
fi

if [[ -n "${ENV_SOURCE}" ]]; then
  if [[ ! -f "${ENV_SOURCE}" ]]; then
    echo "[ERROR] ENV_SOURCE does not exist: ${ENV_SOURCE}"
    exit 1
  fi
  log "Updating env file from ${ENV_SOURCE}"
  install -m 600 -o root -g root "${ENV_SOURCE}" "${ENV_DEST}"
fi

if [[ ! -f "${ENV_DEST}" ]]; then
  echo "[ERROR] Missing env file: ${ENV_DEST}"
  echo "        Create it first (or pass ENV_SOURCE=/path/to/hotel_service.env)."
  exit 1
fi

log "Deploying backend jar"
install -m 640 -o "${APP_USER}" -g "${APP_GROUP}" "${JAR_SRC}" "${APP_DIR}/hotel_service.jar"

log "Deploying frontend static files"
rm -rf "${WEB_DIR:?}"/*
cp -r "${FRONTEND_SRC}"/* "${WEB_DIR}/"
chown -R www-data:www-data "${WEB_DIR}"

log "Restarting backend service"
systemctl daemon-reload
systemctl restart "${SERVICE_NAME}"
systemctl is-active --quiet "${SERVICE_NAME}" || {
  echo "[ERROR] ${SERVICE_NAME} failed to start"
  systemctl --no-pager -l status "${SERVICE_NAME}" || true
  journalctl -u "${SERVICE_NAME}" -n 100 --no-pager || true
  exit 1
}

log "Validating and reloading nginx"
nginx -t
systemctl reload nginx

log "Smoke checks"
curl -fsS "http://127.0.0.1:9000/actuator/health" >/dev/null 2>&1 || log "Health endpoint check skipped/failed (verify actuator if disabled)."
curl -fsS "http://127.0.0.1/" >/dev/null 2>&1 || log "Frontend local check failed; verify nginx root and permissions."

log "Deploy completed successfully"
log "Backend: ${APP_DIR}/hotel_service.jar"
log "Frontend: ${WEB_DIR}"

