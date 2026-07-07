# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Single-activity Android app (Java, package `org.ameelio.pdfviewer`) that renders PDFs with Android's native `PdfRenderer` API.

**Security Design**: Built for high-security environments (such as correctional facilities) where user input poses risks. There are no text input fields, no keyboard interactions, no network permission, and no persistence of state or history. This constraint drives most design decisions — do not weaken it:

- Only permission is `READ_EXTERNAL_STORAGE`; never add network or other permissions.
- `onSaveInstanceState`/`onRestoreInstanceState` in `PdfViewerActivity` intentionally drop all state (empty Bundle). This is a security requirement, not a bug.
- `android:allowBackup="false"` in the manifest.
- The file picker uses `ACTION_OPEN_DOCUMENT` with `EXTRA_LOCAL_ONLY` and saves no recents/history.
- Security regressions are guarded by `SecurityTest`; extend it when touching persistence, permissions, or intent handling.

## Code Principles

- Always update documentation (README.md, CLAUDE.md, AGENTS.md) when a code change is relevant to it. AGENTS.md holds additional contributor guidelines (naming conventions, commit/PR expectations).
- Unless specified otherwise, prefer writing tests for new code functionality.
- User-facing changes are recorded in Changelog.md (Keep a Changelog format).

## Build Commands

- **Container build (recommended)**: `./scripts/build.sh` — builds via Podman and outputs `ameelio-pdf-viewer.apk` (release) and `ameelio-pdf-viewer-debug.apk` (debug) at the repo root.
- **Direct Gradle build**: `./gradlew assembleDebug assembleRelease` (requires local Android SDK 34).
- **Clean**: `./gradlew clean`

The Dockerfile provides the build environment (Eclipse Temurin JDK 17 + Android SDK 34, checksum-verified CLI tools); its default command runs `assembleDebug assembleRelease`.

## Testing Commands

- **All tests in container (recommended)**: `./scripts/test.sh` — runs unit tests via Podman and extracts XML/HTML reports to `./test-reports/`.
- **Unit tests**: `./gradlew test`
- **Lint + unit tests**: `./gradlew check`
- **Single test class**: `./gradlew testDebugUnitTest --tests "org.ameelio.pdfviewer.SecurityTest"`
- **Instrumentation tests**: `./gradlew connectedAndroidTest` (requires device/emulator)

Unit tests (`app/src/test/`) use JUnit 4 + Robolectric (`@RunWith(RobolectricTestRunner.class)`, `@Config(sdk = 34)`) and Mockito. Instrumentation tests (`app/src/androidTest/`) use Espresso and espresso-intents. There is no JaCoCo setup — `jacocoTestReport` is not a valid task.

Robolectric cannot create or render PDFs (`PdfDocument`/`PdfRenderer` natives are unavailable, so they no-op or throw). Unit tests must not depend on opening a real document; anything that exercises actual PDF rendering belongs in instrumentation tests or manual emulator verification.

## Device / Manual QA Scripts

These require `adb` and a connected device or emulator:

- `./scripts/reinstall.sh` / `./scripts/reinstall-debug.sh` — uninstall both app variants, then install the release/debug APK from the repo root.
- `./scripts/rebuild-and-reinstall.sh` — chains `build.sh`, `reinstall-debug.sh`, and `watch-logs.sh`.
- `./scripts/watch-logs.sh [--debug] [logcat flags...]` — stream logcat output for the app.

## Architecture

All logic lives in `app/src/main/java/org/ameelio/pdfviewer/` — five classes total.

### Rendering pipeline (`PdfViewerActivity`)

Single activity with `launchMode="singleTask"`. PDFs arrive either through `ACTION_VIEW` intents (registered for `application/pdf` MIME type and `file`/`content` URIs) or the built-in file picker; `onNewIntent` handles opening a new PDF while one is already loaded (the old renderer and file descriptor are swapped out via `replaceRenderer`).

Pages render lazily through a RecyclerView: the inner `PdfPageAdapter` renders each page to a bitmap on bind, scaled to screen width and capped at `MAX_RENDER_DIMENSION` (2048px) to prevent OOM. Bitmaps are kept in a small cache (`MAX_CACHED_PAGES` = 3) with scroll-driven eviction (`cleanupDistantPages` keeps ±2 pages around the viewport). Evicted bitmaps are deliberately **not** `recycle()`d — they may still be on screen; GC reclaims them.

### Zoom system (four collaborating classes)

- **`PinchZoomItemTouchListener`** — a `RecyclerView.OnItemTouchListener` that detects pinches at the RecyclerView level. This is the primary pinch path: item touch listeners see every event even after RecyclerView has intercepted for scrolling, so pinches that begin mid-scroll still zoom (per-child detection alone misses them — the child gets `ACTION_CANCEL` when scrolling starts).
- **`ZoomableImageView`** — per-page view that acts as a gesture sensor for single-finger pan-while-zoomed (it also carries a pinch detector, but the RecyclerView-level listener claims multi-touch gestures first). It never transforms itself.
- **`ZoomCoordinator`** — hub holding the single document-wide scale; broadcasts scale and pan deltas to weakly-referenced listeners. Owns the shared scale limits `MIN_SCALE`/`MAX_SCALE` ([0.5, 5.0]).
- **`DocumentZoomController`** — the listener that actually applies transforms: scales/translates the entire RecyclerView as one canvas, clamps translation to the visible overflow, and forwards vertical pan overflow to `scrollBy()` so scrolling continues seamlessly while zoomed.

The activity's zoom in/out/reset buttons drive the same coordinator (`applyZoomDelta`) in 0.25 steps. `recyclerView.setMotionEventSplittingEnabled(false)` keeps pinch gestures that span two pages working. Design history and rationale: `docs/ZOOM_NOTES.md`.

## Build Configuration

- Target/compile SDK 34, min SDK 21, Java 8 compatibility, AGP 8.1.4.
- Dependencies: AndroidX AppCompat, ConstraintLayout, RecyclerView — nothing else at runtime.
- Debug builds append `.debug` to the application id, so debug and release can be installed side by side (the reinstall scripts uninstall both).
- Release builds are signed with the debug key (`signingConfig signingConfigs.debug`) — suitable for sideloading, not store distribution.

## Installation

- `adb install ameelio-pdf-viewer.apk` (release) or `adb install ameelio-pdf-viewer-debug.apk` (debug), or use the reinstall scripts above.
