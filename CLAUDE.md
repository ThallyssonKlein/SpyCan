# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

SpyCan is a single-module Android app that records video **in the background** — with no on-screen camera preview — and saves clips to shared storage (`Movies/SpyCan`). The UI has two buttons, one per lens (back/front); tapping one starts recording on that camera and turns it into a stop button, while the other lens' button is disabled until recording stops.

## Commands

Build/test run through the Gradle wrapper (`./gradlew`).

- **Build debug APK:** `./gradlew assembleDebug` (output in `app/build/outputs/apk/debug/`)
- **Install on a connected device/emulator:** `./gradlew installDebug`
- **Full build + lint:** `./gradlew build`
- **Android Lint only:** `./gradlew lint` (report in `app/build/reports/lint-results-*.html`)
- **Unit tests:** `./gradlew test` — **no test sources exist yet**; there is no `src/test` or `src/androidTest`. A single unit test class runs via `./gradlew :app:testDebugUnitTest --tests "com.tk.spycan.SomeTest"`.

There is no CI, no ktlint/detekt, and no README beyond a one-line description. Lint is the only static analysis wired up.

## Toolchain / versions

- Android Gradle Plugin 8.5.2, Kotlin 1.9.24 (versions declared in root `build.gradle.kts`)
- `compileSdk`/`targetSdk` 34, `minSdk` 26, Java/JVM target 17
- View Binding is enabled (`buildFeatures { viewBinding = true }`); layouts are accessed via generated `*Binding` classes (e.g. `ActivityMainBinding`), not `findViewById`.
- `local.properties` (SDK path) is machine-specific and gitignored. Release signing reads `keystore.properties` at the repo root (also gitignored) if present; `app/build.gradle.kts` only wires up the `release` signing config when that file exists, so its absence is expected on a fresh checkout and release builds simply go unsigned.

## Architecture

Two Kotlin files under `app/src/main/java/com/tk/spycan/` do all the work:

- **`MainActivity`** — thin UI layer. Requests runtime permissions (CAMERA is mandatory; RECORD_AUDIO and POST_NOTIFICATIONS are best-effort), then starts/stops `RecordingService` via `startForegroundService` with `ACTION_START` (carrying an `EXTRA_LENS` of `LENS_BACK`/`LENS_FRONT`) / `ACTION_STOP` intents. It reads `RecordingService.isRecording` and `RecordingService.lensFacing` to render which of the two lens buttons is active, and re-syncs in `onResume`.

- **`RecordingService`** — the core. It is a **`LifecycleService`** specifically because CameraX's `bindToLifecycle` needs a `LifecycleOwner`, and using the service as that owner is what lets recording run without any UI/preview. Flow: promote to foreground (posting an ongoing notification) *before* touching the camera → get `ProcessCameraProvider` → build a `Recorder`/`VideoCapture` → `unbindAll()` then bind to whichever lens (`CameraSelector.DEFAULT_BACK_CAMERA`/`DEFAULT_FRONT_CAMERA`) was requested → write to `MediaStore` (`Movies/SpyCan`, timestamped `SpyCan_yyyyMMdd_HHmmss.mp4`).

### Cross-cutting constraints to respect when editing

- **Foreground-service ordering matters.** On modern Android you must call `startForeground` before accessing the camera from the background. Keep `startAsForeground()` ahead of any camera work in `startRecording()`.
- **Foreground service type must match granted permissions.** `startAsForeground()` sets `FOREGROUND_SERVICE_TYPE_CAMERA` and only ORs in `..._MICROPHONE` when RECORD_AUDIO is actually granted. Audio is likewise only enabled on the recording (`withAudioEnabled()`) when permission is present — declaring a microphone service type without the runtime permission crashes on start. The manifest declares `foregroundServiceType="camera|microphone"` plus the matching `FOREGROUND_SERVICE_*` permissions; keep the three (manifest type, runtime service type, actual usage) consistent.
- **`isRecording` and `lensFacing` are the single source of truth** for recording state, shared Activity↔Service. Both are `@Volatile` vars with a private setter in `RecordingService.companion`; `isRecording` is flipped from CameraX `VideoRecordEvent` callbacks (`Start`/`Finalize`), `lensFacing` is set when the camera binds. The UI polls these rather than binding to the service.
- **Only one lens records at a time.** `startRecording(lens)` is a no-op if `isRecording` is already true, and the camera bind path always calls `provider.unbindAll()` first — there's no concurrent front+back capture.
- **Storage is SDK-gated.** Output uses `MediaStore` with `RELATIVE_PATH` on API ≥ 29 (Q); the `WRITE_EXTERNAL_STORAGE` permission in the manifest is capped at `maxSdkVersion="28"` for older devices.

User-facing strings live in `app/src/main/res/values/strings.xml`; the single layout is `activity_main.xml`.
