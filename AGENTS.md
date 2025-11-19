# Repository Guidelines

## Directions for Agents
- Prefer to use framework-provided modules whenever possible, and only write custom implementations if the built-in modules aren't sufficient to accomplish the task
- Always write tests when making a code change, unless testing that code change doesn't make sense
- If a request is unclear, ask for clarification

## Project Structure, Architecture & Security
- Single Android module (`app/`); keep logic in `app/src/main/java/org/ameelio/pdfviewer`, resources in `app/src/main/res`, manifest edits in `app/src/main/AndroidManifest.xml`, tests in `app/src/{test,androidTest}/java`, and container artifacts in `test-reports/`.
- Architecture stays single-activity: `PdfViewerActivity` renders PDFs through `PdfRenderer`, streams bitmaps into a RecyclerView, and never saves state. Preserve `android:allowBackup="false"`, forbid keyboard/input widgets, and keep permissions limited to `READ_EXTERNAL_STORAGE`. Respect cache bounds (`MAX_CACHED_PAGES`, `MAX_RENDER_DIMENSION`) to avoid OOM or leaked data.
- Podman scripts (`scripts/build.sh`, `scripts/test.sh`, `scripts/reinstall.sh`) mirror the Dockerfile stack (Temurin JDK 17 + SDK 34). Update scripts plus README.md/CLAUDE.md/AGENTS.md together whenever workflows change.

## Build, Test & Coverage Commands
- `./scripts/build.sh` — container build producing `ameelio-pdf-viewer.apk` and `ameelio-pdf-viewer-debug.apk`.
- `./gradlew assembleDebug assembleRelease` — local SDK build; add `./gradlew clean` when artifacts misbehave.
- `./scripts/test.sh` — containerized `./gradlew check` plus security smoke tests, exporting XML/HTML to `test-reports/`. Prefer using this script when running tests
- `./gradlew check` — lint + unit tests; must pass before pushing.
- `./gradlew connectedAndroidTest` — run Espresso flows on an emulator/device.
- `./gradlew testDebugUnitTest --tests "org.ameelio.pdfviewer.SecurityTest"` and `./gradlew testDebugUnitTest jacocoTestReport` — focused security runs and coverage generation.
- `./scripts/reinstall.sh` — uninstall/install latest APK via adb for manual QA.

## Coding Style & Naming
- Java 8, 4-space indentation, braces on the same line, explicit `@Override`. Classes PascalCase, methods camelCase, constants `UPPER_SNAKE_CASE` (`MAX_CACHED_PAGES`), resource ids `lowercase_with_underscores`, and layouts `activity_*`.
- Prefer immutable fields, AndroidX APIs, and guardrails against persistence. Any new functionality must include or update tests and lint is expected to be clean before commit.

## Testing Expectations
- Unit tests rely on JUnit4 + Robolectric; name them `<Feature>Test.java` (e.g., `SecurityTest`). Instrumentation specs use Espresso/`AndroidJUnitRunner` and end with `InstrumentedTest`.
- Cover file-picker intents, PdfRenderer lifecycle, bitmap cache eviction, permission handling, and anything that could weaken the “no persistence” guarantee. Attach emulator logs or screenshots to PRs when UI or security flows change.

## Commit & PR Workflow
- Keep commits short, imperative, and single-purpose (`Fix gradle cache in Dockerfile`). Describe non-trivial verification commands in the body and rerun `./scripts/build.sh` or `./gradlew check` afterward.
- PRs must explain the security/user impact, list the commands run, link issues, and include before/after screenshots for UI tweaks. Call out manifest or permission changes explicitly and highlight any deviation from the “no input / no network / no storage” rule.
- Always refresh README.md, CLAUDE.md, and AGENTS.md when behavior or contributor processes change so downstream agents remain accurate.
