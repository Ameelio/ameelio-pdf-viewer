#!/usr/bin/env bash

set -euo pipefail

APP_ID="org.ameelio.pdfviewer"
ADB_BIN="${ADB:-adb}"
LOGCAT_ARGS=()
LOG_PREFIX="[watch-logs]"

log() {
    echo "${LOG_PREFIX} $*" >&2
}

show_help() {
    cat <<EOF
Usage: $0 [--debug] [logcat options...]

Streams logcat output for the Ameelio PDF Viewer app from the first connected
device/emulator. Pass any additional logcat flags after the optional --debug
switch. Example: $0 -v time
EOF
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --debug)
            APP_ID="${APP_ID}.debug"
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            LOGCAT_ARGS+=("$1")
            shift
            ;;
    esac
done
log "Target application id: $APP_ID"

if ! command -v "$ADB_BIN" >/dev/null 2>&1; then
    log "adb not found. Please ensure Android platform-tools are in your PATH."
    exit 1
fi

log "Using adb binary at $(command -v "$ADB_BIN")"

if ! "$ADB_BIN" get-state >/dev/null 2>&1; then
    log "No connected device/emulator detected. Output of 'adb devices':"
    "$ADB_BIN" devices >&2 || true
    echo "Run \`adb devices\` to verify connectivity." >&2
    exit 1
fi

log "Looking for process id of $APP_ID..."
# Attempt to collect logcat output scoped to our package, even if we cannot resolve a pid yet.
log "Attempting to resolve process id via 'adb shell pidof -s'..."
set +e
PID_OUTPUT="$("$ADB_BIN" shell pidof -s "$APP_ID" 2>&1)"
PID_STATUS=$?
set -e
PID="$(echo "$PID_OUTPUT" | tr -d '\r' | xargs)"
log "pidof exit=$PID_STATUS output='${PID_OUTPUT//$'\n'/ }'"
if [[ $PID_STATUS -ne 0 ]]; then
    log "pidof failed; falling back to logcat filter."
fi

if [[ -n "$PID" ]]; then
    log "Found pid: $PID"
    LOGCAT_PID_ARGS=(--pid="$PID")
else
    log "pidof returned empty result; app may not be running yet."
    log "Falling back to LOGCAT tag/package filter for $APP_ID."
    LOGCAT_PID_ARGS=(-s "$APP_ID" "AndroidRuntime" "ZoomableImageView")
fi

if [[ -n "$PID" ]]; then
    log "Streaming logcat for $APP_ID (pid $PID). Press Ctrl-C to stop..."
else
    log "Streaming logcat for $APP_ID using best-effort filter (start the app to see logs). Press Ctrl-C to stop..."
fi

if [[ ${#LOGCAT_ARGS[@]} -gt 0 ]]; then
    log "Command: $ADB_BIN logcat ${LOGCAT_PID_ARGS[*]} ${LOGCAT_ARGS[*]}"
else
    log "Command: $ADB_BIN logcat ${LOGCAT_PID_ARGS[*]} (no extra logcat args)"
fi
exec "$ADB_BIN" logcat "${LOGCAT_PID_ARGS[@]}" "${LOGCAT_ARGS[@]}"
