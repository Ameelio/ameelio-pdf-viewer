#!/bin/bash

# Build script for PDF Viewer Android App
# This script builds the APK using Podman (rootless) and copies it to the main directory

set -e

echo "Building PDF Viewer Android App..."

# Build container image with Podman
echo "Building container image..."
podman build -t pdf-viewer-android .

# Clean up any existing container with the same name
echo "Cleaning up existing containers..."
podman rm -f pdf-viewer-build 2>/dev/null || true

# Run container to build APK (with output visible)
echo "Running build in container..."
CONTAINER_ID=$(podman run --name pdf-viewer-build pdf-viewer-android)

# Copy the APKs from container to host
echo "Extracting APKs from container..."
podman cp pdf-viewer-build:/app/app/build/outputs/apk/debug/app-debug.apk ./ameelio-pdf-viewer-debug.apk
podman cp pdf-viewer-build:/app/app/build/outputs/apk/release/app-release.apk ./ameelio-pdf-viewer.apk

# Clean up container
podman rm pdf-viewer-build

echo "✅ Build complete! APKs saved as:"
echo "  - ameelio-pdf-viewer.apk (release)"
echo "  - ameelio-pdf-viewer-debug.apk (debug)"
echo "📱 Install with: adb install ameelio-pdf-viewer.apk"