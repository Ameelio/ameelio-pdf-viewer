#!/bin/bash

# Build script for PDF Viewer Android App
# This script builds the APK using Docker and copies it to the main directory

set -e

echo "Building PDF Viewer Android App..."

# Build Docker image
echo "Building Docker image..."
docker build -t pdf-viewer-android .

# Create container and copy APK
echo "Extracting APK from container..."
CONTAINER_ID=$(docker create pdf-viewer-android)

# Copy the APK from container to host
docker cp $CONTAINER_ID:/app/app/build/outputs/apk/debug/app-debug.apk ./pdf-viewer.apk

# Clean up container
docker rm $CONTAINER_ID

echo "✅ Build complete! APK saved as: pdf-viewer.apk"
echo "📱 Install with: adb install pdf-viewer.apk"