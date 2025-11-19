#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "$REPO_ROOT"

# Build script for PDF Viewer Android App
# This script builds the APK using Podman (rootless) and copies it to the main directory

echo "Building PDF Viewer Android App..."

# Build container image with Podman
echo "Building container image..."
podman build -t ameelio-pdf-viewer .

# Clean up any existing container with the same name
echo "Cleaning up existing containers..."
podman rm -f ameelio-pdf-viewer-build 2>/dev/null || true

# Run container to build APK (with output visible)
echo "Running build in container..."
if podman run --name ameelio-pdf-viewer-build ameelio-pdf-viewer; then
    # Copy the APKs from container to host
    echo "Extracting APKs from container..."
    
    # Copy debug APK
    if podman cp ameelio-pdf-viewer-build:/app/app/build/outputs/apk/debug/app-debug.apk ./ameelio-pdf-viewer-debug.apk; then
        echo "✅ Debug APK copied successfully"
    else
        echo "❌ Failed to copy debug APK"
    fi
    
    # Copy release APK
    if podman cp ameelio-pdf-viewer-build:/app/app/build/outputs/apk/release/app-release.apk ./ameelio-pdf-viewer.apk; then
        echo "✅ Release APK copied successfully"
    else
        echo "❌ Failed to copy release APK"
    fi
    
    # Clean up container
    podman rm ameelio-pdf-viewer-build
else
    echo "❌ Build failed!"
    podman rm ameelio-pdf-viewer-build 2>/dev/null || true
    exit 1
fi

echo "✅ Build complete! APKs saved as:"
echo "  - ameelio-pdf-viewer.apk (release)"
echo "  - ameelio-pdf-viewer-debug.apk (debug)"
echo "📱 Install with: adb install ameelio-pdf-viewer.apk"
