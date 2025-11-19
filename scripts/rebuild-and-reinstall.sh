#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "$REPO_ROOT"

echo "Rebuilding the application..."
"${SCRIPT_DIR}/build.sh"

echo "Reinstalling the application in debug mode..."
"${SCRIPT_DIR}/reinstall-debug.sh"

echo "Tailing the application logs..."
"${SCRIPT_DIR}/watch-logs.sh"
