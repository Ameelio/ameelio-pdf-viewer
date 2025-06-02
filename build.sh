#!/bin/bash

# Build script for PDF Viewer Android App
# This script builds the APK using Podman (rootless) and copies it to the main directory

set -e

echo "Building PDF Viewer Android App..."

# Build container image with Podman
echo "Building container image..."
podman build -t pdf-viewer-android .

# Create container and copy APK
echo "Extracting APK from container..."
CONTAINER_ID=$(podman create pdf-viewer-android)

# Copy the APK from container to host
podman cp $CONTAINER_ID:/app/app/build/outputs/apk/debug/app-debug.apk ./pdf-viewer.apk

# Clean up container
podman rm $CONTAINER_ID

echo "✅ Build complete! APK saved as: pdf-viewer.apk"
echo "📱 Install with: adb install pdf-viewer.apk"