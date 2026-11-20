# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

SpyCam is a single-module Android app that records video **in the background** — with no on-screen camera preview — and saves clips to shared storage (`Movies/SpyCam`). The UI has two buttons, one per lens (back/front); tapping one starts recording on that camera and turns it into a stop button, while the other lens' button is disabled until recording stops.

## Commands

Build/test run through the Gradle wrapper (`./gradlew`).

- **Build debug APK:** `./gradlew assembleDebug` (output in `app/build/outputs/apk/debug/`)
- **Install on a connected device/emulator:** `./gradlew installDebug`
- **Full build + lint:** `./gradlew build`
- **Android Lint only:** `./gradlew lint` (report in `app/build/reports/lint-results-*.html`)
- **Format:** `./gradlew ktlintFormat` (rules in `.editorconfig`). **Static analysis:** `./gradlew detekt lintDebug` (detekt config in `config/detekt/detekt.yml`).
- **Unit tests:** `./gradlew testDebugUnitTest` (JUnit 5 + MockK + Turbine). A single class: `./gradlew :app:testDebugUnitTest --tests "com.tk.spycam.RecordingSessionTest"`.
- **Coverage:** `./gradlew koverHtmlReportDebug`; `./gradlew build` runs `koverVerify` (>= 80% line/branch/instruction). Android glue (Activity, Service, Application, `CameraXRecorder`, `RecordingControls`) is excluded in `app/build.gradle.kts`.

There is no CI beyond the CodeQL workflow. Dependency and plugin versions live in `gradle/libs.versions.toml`.

## Toolchain / versions

- Android Gradle Plugin 8.5.2, Kotlin 1.9.24 (versions declared in `gradle/libs.versions.toml`; JDK toolchain 17 set in `app/build.gradle.kts`)
- `compileSdk`/`targetSdk` 34, `minSdk` 26, Java/JVM target 17
- View Binding is enabled (`buildFeatures { viewBinding = true }`); layouts are accessed via generated `*Binding` classes (e.g. `ActivityMainBinding`), not `findViewById`.
- `local.properties` (SDK path) is machine-specific and gitignored. Release signing reads `keystore.properties` at the repo root (also gitignored) if present; `app/build.gradle.kts` only wires up the `release` signing config when that file exists, so its absence is expected on a fresh checkout and release builds simply go unsigned.

## Architecture

Everything is under `app/src/main/java/com/tk/spycam/`. The logic lives in small testable classes; Android glue stays thin.

- **UI:** `MainActivity` (permissions, starts/stops the service, observes state) + `MainViewModel` (turns a lens tap into a `MainCommand`: start or stop) + `RecordingControls` (renders `RecordingState` into the two buttons).
- **Service:** `RecordingService` is a `LifecycleService` — being the `LifecycleOwner` is what lets CameraX record without a preview. It promotes itself to foreground, builds a `RecordingSession`, and reacts to its `RecordingOutcome`s (toast, then stop itself). Intents are built via `RecordingService.startIntent/stopIntent`.
- **Recording logic:** `RecordingSession` runs the `CameraRecorder.record(lens)` flow in the service scope, updates `RecordingStateHolder`, reports metrics, and emits `RecordingOutcome`s. `CameraXRecorder` is the CameraX implementation of `CameraRecorder` (wraps provider/bind failures in `CameraUnavailableException`).
- **Shared state:** `RecordingStateHolder` (a `StateFlow<RecordingState>`: `Idle` / `Recording(lens)`) is created once in `SpyCamApplication`, which also holds `Metrics` (`TimberMetrics`), and is read by both the ViewModel and the service. There is no static mutable state.

### Cross-cutting constraints to respect when editing

- **Foreground-service ordering matters.** `RecordingService.startRecording` must call `startAsForeground()` before `session.start(lens)` touches the camera.
- **Foreground service type must match granted permissions.** `resolveForegroundServiceType()` only adds `..._MICROPHONE` when RECORD_AUDIO is granted, and `CameraXRecorder` only enables audio under the same check. The manifest declares `camera|microphone` plus the matching `FOREGROUND_SERVICE_*` permissions; keep the three consistent.
- **State flips on CameraX events.** `RecordingStateHolder` becomes `Recording` on the `Start` event and `Idle` when the session's flow ends (`finally`), so the UI reflects the real recording, not the tap.
- **Only one lens records at a time.** `RecordingSession.start` ignores a start while a recording job is active, and `CameraXRecorder` calls `unbindAll()` before binding.
- **Storage is SDK-gated.** Output uses `MediaStore` with `RELATIVE_PATH` on API >= 29; `WRITE_EXTERNAL_STORAGE` is capped at `maxSdkVersion="28"`.
- **Project rules** live in `.claude/rules/` (clean code, observability, testing, verify-after-edit). Logs use Timber with a `[ClassName]` prefix; tests keep mocks inside each test function.

User-facing strings live in `app/src/main/res/values/strings.xml`; the single layout is `activity_main.xml`.
