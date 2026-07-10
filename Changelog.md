# Changelog

All notable changes to the Ameelio PDF Viewer will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
