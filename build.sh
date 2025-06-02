#!/bin/bash

# Build script for PDF Viewer Android App
# This script builds the APK using Podman (rootless) and copies it to the main directory

set -e

echo "Building PDF Viewer Android App..."

# Build container image with Podman
echo "Building container image..."
podman build -t pdf-viewer-android .

# Run container to build APK (with output visible)
echo "Running build in container..."
CONTAINER_ID=$(podman run --name pdf-viewer-build pdf-viewer-android)

# Copy the APK from container to host
echo "Extracting APK from container..."
podman cp pdf-viewer-build:/app/app/build/outputs/apk/debug/app-debug.apk ./pdf-viewer.apk

# Clean up container
podman rm pdf-viewer-build

echo "✅ Build complete! APK saved as: pdf-viewer.apk"
echo "📱 Install with: adb install pdf-viewer.apk"