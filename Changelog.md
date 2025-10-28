# Changelog

All notable changes to the Ameelio PDF Viewer will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]


## [1.1] - 2025-10-28

### Fixed
- Fixed ANR (Application Not Responding) crash when opening very large PDFs with 500+ pages
  - Previously, the app would crash after successfully opening large PDFs (e.g., 634MB with 581 pages)
  - Root cause: Synchronously creating hundreds of View objects on the main UI thread blocked it for ~3 seconds
  - System would force-kill the app with "Force finishing activity" after UI freeze
  - The app now creates placeholder views asynchronously or uses lazy initialization to prevent blocking the main thread

## [1.0] - 2025-07-06

- Initial implementation
