# Changelog

All notable changes to the Ameelio PDF Viewer will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Security
- Security (build tooling only, nothing ships in the APK): force the patched dependency versions
  from `settings.gradle.kts` so they also apply to the dependency graph submitted to GitHub, and
  add Kotlin Gradle Plugin 2.4.21 and Apache HttpClient 4.5.14.
- Security (build tooling only): apply the same forced versions to every project configuration,
  not just the buildscript classpath, so AGP's lint tool classpath no longer resolves vulnerable
  Bouncy Castle, Guava, commons-lang3 and HttpClient.

### Fixed

- Lint `AppLinkUrlError`: add `android:host="*"` to the `file://` PDF intent filter.

## [1.1.2] - 2026-11-16

### Changed
- Raise `minSdk` to 34 (Android 14), uniform across the tablet apps; older devices can no longer install.

### Security
- Upgraded Android Gradle Plugin 8.1.4 -> 9.5.1 (Gradle 8.0 -> 9.6.0) and pinned patched versions of build-classpath libraries (netty, bouncycastle, guava, jose4j, jdom2, commons-lang3, protobuf) and of the Robolectric test classpath (guava, bouncycastle) to close Dependabot alerts. Build and test tooling only; nothing here ships in the APK and the app's behavior is unchanged.
- Modernized the build to match the other tablet apps: Gradle files converted to Kotlin DSL (`*.gradle.kts`), Java source/target 8 -> 17 (matches the container's JDK 17 and removes the obsolete-option compiler warnings), and Jetifier removed (no longer needed).
- The container image now also installs `build-tools;36.0.0`, which AGP 9 expects.

### Fixed
- Fixed pinch-to-zoom not working when the gesture starts during or after a scroll
  - Previously, once a finger dragged far enough to start scrolling, RecyclerView took over the gesture and the per-page views never saw the second finger, so the pinch was ignored; only a perfectly simultaneous two-finger pinch worked
  - Pinch detection now also runs at the RecyclerView level (`PinchZoomItemTouchListener`), which sees every touch event regardless of scroll state
- Fixed `DocumentZoomController` re-posting itself every frame while no PDF is open (the RecyclerView has no size yet); it now waits for a layout change instead, which also re-clamps the zoom translation when the view's size changes
- Fixed the unit test suite silently not running
  - Robolectric 4.9 does not support the configured SDK 34, so every test was reported as "skipped" while the build still passed; upgraded to Robolectric 4.11.1
  - Enabled `includeAndroidResources` so Robolectric tests can create the real activity (previously failed with a missing AppCompat theme)
  - Excluded Robolectric's `nativeruntime-dist-compat` from Jetifier, which cannot transform it

## [1.1] - 2025-10-28

### Fixed
- Fixed ANR (Application Not Responding) crash when opening very large PDFs with 500+ pages
  - Previously, the app would crash after successfully opening large PDFs (e.g., 634MB with 581 pages)
  - Root cause: Synchronously creating hundreds of View objects on the main UI thread blocked it for ~3 seconds
  - System would force-kill the app with "Force finishing activity" after UI freeze
  - The app now creates placeholder views asynchronously or uses lazy initialization to prevent blocking the main thread

## [1.0] - 2025-07-06

- Initial implementation
