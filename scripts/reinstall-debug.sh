#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

APK_NAME="ameelio-pdf-viewer-debug.apk"
APK_PATH="${REPO_ROOT}/${APK_NAME}"
PKG_RELEASE="org.ameelio.pdfviewer"
PKG_DEBUG="${PKG_RELEASE}.debug"

echo "[reinstall-debug] Uninstalling $PKG_RELEASE (ignore errors if not installed)..."
adb uninstall "$PKG_RELEASE" >/dev/null 2>&1 || true

echo "[reinstall-debug] Uninstalling $PKG_DEBUG (ignore errors if not installed)..."
adb uninstall "$PKG_DEBUG" >/dev/null 2>&1 || true

echo "[reinstall-debug] Installing $APK_NAME..."
adb install "$APK_PATH"
