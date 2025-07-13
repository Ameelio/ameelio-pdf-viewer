# PDF Viewer Android

<p align="center">
  <img src="app/src/main/res/mipmap-hdpi/ic_launcher.png" alt="PDF Viewer Logo" width="128" height="128">
</p>

A simple, secure PDF viewer for Android designed specifically for environments where user input poses security risks, such as correctional facilities.

## Project Goals

This PDF viewer is designed with security and simplicity in mind:

- **No user input**: The app does not accept any user input, keyboard interactions, or text entry
- **Read-only PDF display**: Opens and displays PDF files without any editing capabilities
- **Secure by design**: Minimal attack surface for high-security environments
- **Simple interface**: Clean, straightforward PDF viewing with automatic page scaling

## Features

- Opens PDF files from file managers, email attachments, and other apps
- Displays all pages in a scrollable view
- Automatically scales pages to fit screen width
- Supports standard PDF files via Android's native PdfRenderer API
- No network permissions or external dependencies

## Build Instructions

### Prerequisites

- [Podman](https://podman.io/) (for containerized builds)
- OR Android SDK with API level 34 (for direct builds)

### Container Build (Recommended)

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd pdf-viewer-android
   ```

2. Run the build script:
   ```bash
   ./build.sh
   ```

   This will:
   - Build a containerized Android development environment
   - Compile both debug and release APKs
   - Output `ameelio-pdf-viewer.apk` (release) and `ameelio-pdf-viewer-debug.apk` (debug)

### Direct Build

If you have Android SDK installed:

```bash
./gradlew assembleDebug assembleRelease
```

## Installation

### Install on Device

1. Enable "Unknown Sources" in your Android device settings
2. Install the APK:
   ```bash
   adb install ameelio-pdf-viewer.apk
   ```

### Usage

1. Open any PDF file from your file manager, email, or other app
2. Select "PDF Viewer" when prompted to choose an app
3. The PDF will open and display all pages in a scrollable view
4. Use standard Android navigation (back button) to exit

## Technical Details

- **Target SDK**: Android 14 (API 34)
- **Minimum SDK**: Android 5.0 (API 21)
- **Permissions**: `READ_EXTERNAL_STORAGE` only
- **Architecture**: Single activity with native PDF rendering

## Security Considerations

This app is designed for high-security environments:

- No text input fields or keyboards
- No network access or internet permissions
- No external dependencies beyond Android framework
- Read-only PDF display with no editing capabilities
- Minimal permissions (only file reading)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.