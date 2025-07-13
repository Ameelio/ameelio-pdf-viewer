# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

This project uses Gradle for building Android APKs with container-based builds:

- **Build APKs**: `./build.sh` - Builds both debug and release APKs using Podman container
- **Direct Gradle build**: `./gradlew assembleDebug assembleRelease` - Builds APKs directly (requires Android SDK setup)
- **Clean build**: `./gradlew clean` - Cleans build artifacts

The build script (`build.sh`) uses Podman to create a containerized build environment and outputs:
- `ameelio-pdf-viewer.apk` (release build)
- `ameelio-pdf-viewer-debug.apk` (debug build)

## Architecture

This is a single-activity Android app that renders PDF files using Android's native `PdfRenderer` API.

**Security Design**: This app is designed for high-security environments (such as correctional facilities) where user input poses security risks. It has no user input fields, no keyboard interactions, and no network permissions - providing a read-only PDF viewing experience with minimal attack surface.

### Key Components

- **PdfViewerActivity** (`app/src/main/java/com/pdfviewer/PdfViewerActivity.java`): Main activity that handles PDF rendering
  - Receives PDF URIs via Intent
  - Renders each PDF page as a bitmap in a scrollable LinearLayout
  - Handles file access permissions and error states
  - Automatically scales pages to fit screen width

### Android Configuration

- **Target SDK**: 34 (Android 14)
- **Min SDK**: 21 (Android 5.0)
- **Permissions**: `READ_EXTERNAL_STORAGE` only (no network or other permissions for security)
- **Intent Filters**: Registers as default PDF viewer for:
  - `application/pdf` MIME type
  - File URIs with `.pdf` extension
  - Content URIs with PDF MIME type

### Build Configuration

- Uses Android Gradle Plugin 8.1.0
- Java 8 compatibility
- Two build variants: debug (with `.debug` suffix) and release
- Dependencies: AndroidX AppCompat and ConstraintLayout

### Container Build System

The Dockerfile creates a complete Android build environment:
- Based on Eclipse Temurin JDK 17
- Installs Android SDK 34 and build tools
- Includes checksum verification for Android CLI tools
- Configured to run `assembleDebug` and `assembleRelease` by default

## Installation Commands

- **Install APK**: `adb install ameelio-pdf-viewer.apk`
- **Install debug APK**: `adb install ameelio-pdf-viewer-debug.apk`