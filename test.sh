#!/bin/bash

# Test script for PDF Viewer Android App
# This script runs unit tests using Podman (rootless) and copies test reports to the main directory

set -e

echo "Running tests for PDF Viewer Android App..."

# Build container image with Podman
echo "Building container image..."
podman build -t pdf-viewer-android .

# Clean up any existing container with the same name
echo "Cleaning up existing containers..."
podman rm -f pdf-viewer-test 2>/dev/null || true

# Run container to execute tests (with output visible)
echo "Running tests in container..."
if podman run --name pdf-viewer-test pdf-viewer-android ./gradlew test testDebugUnitTest; then
    # Copy test reports from container to host
    echo "Extracting test reports from container..."
    
    # Create reports directory if it doesn't exist
    mkdir -p ./test-reports
    
    # Copy unit test reports
    if podman cp pdf-viewer-test:/app/app/build/reports/tests/testDebugUnitTest ./test-reports/unit-tests 2>/dev/null; then
        echo "✅ Unit test reports copied successfully"
    else
        echo "⚠️  Unit test reports not found (tests may have failed)"
    fi
    
    # Copy test results XML
    if podman cp pdf-viewer-test:/app/app/build/test-results/testDebugUnitTest ./test-reports/test-results 2>/dev/null; then
        echo "✅ Test results XML copied successfully"
    else
        echo "⚠️  Test results XML not found"
    fi
    
    # Copy Jacoco coverage reports if they exist
    if podman cp pdf-viewer-test:/app/app/build/reports/jacoco ./test-reports/coverage 2>/dev/null; then
        echo "✅ Coverage reports copied successfully"
    else
        echo "ℹ️  Coverage reports not generated"
    fi
    
    # Show test summary
    echo ""
    echo "🧪 Test Summary:"
    echo "=================="
    
    # Extract test results from container logs
    echo "Running test summary extraction..."
    podman logs pdf-viewer-test | grep -E "(BUILD SUCCESSFUL|BUILD FAILED|tests completed|test.*failed)" | tail -10 || true
    
    # Clean up container
    podman rm pdf-viewer-test
    
    echo ""
    echo "✅ Tests completed successfully!"
    echo "📊 Test reports saved to ./test-reports/"
    echo "📂 View HTML reports: open ./test-reports/unit-tests/index.html"
    
else
    echo "❌ Tests failed!"
    
    # Try to extract test failure information
    echo "Extracting test failure information..."
    podman logs pdf-viewer-test | grep -A 10 -B 5 -E "(FAILED|ERROR|Exception)" | tail -20 || true
    
    # Still try to copy any reports that were generated
    mkdir -p ./test-reports
    podman cp pdf-viewer-test:/app/app/build/reports/tests/testDebugUnitTest ./test-reports/unit-tests 2>/dev/null || true
    podman cp pdf-viewer-test:/app/app/build/test-results/testDebugUnitTest ./test-reports/test-results 2>/dev/null || true
    
    podman rm pdf-viewer-test 2>/dev/null || true
    exit 1
fi

echo ""
echo "📋 Available test commands:"
echo "  - Unit tests only: ./test.sh unit"
echo "  - Security tests only: ./test.sh security"
echo "  - All tests with coverage: ./test.sh coverage"