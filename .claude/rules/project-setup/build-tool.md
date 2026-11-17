# Build Tool Rule

Always use **Gradle through the Gradle Wrapper (`./gradlew`)** with the **Kotlin DSL** (`build.gradle.kts`). Never use Maven, Ant, or a globally installed `gradle`.

## Hard Rules

1. **Use `./gradlew` for all build and dependency commands.**
   The wrapper pins the Gradle version so every machine and CI agent builds identically.

   ```bash
   # ✅ Correct
   ./gradlew assembleDebug
   ./gradlew testDebugUnitTest
   ./gradlew dependencies
   ./gradlew ktlintFormat

   # ❌ Wrong
   gradle build
   mvn package
   ```

2. **Use Kotlin DSL only — never Groovy `build.gradle`.**
   Build scripts are `build.gradle.kts` and `settings.gradle.kts`.

3. **Declare every dependency and plugin version in the version catalog (`gradle/libs.versions.toml`).**
   Never inline versions in module build files and never use `+`, `latest.release`, or version ranges.

   ```toml
   # ✅ Correct — pinned in the catalog
   [versions]
   coroutines = "1.9.0"

   [libraries]
   kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
   ```

   ```kotlin
   // ✅ Correct
   implementation(libs.kotlinx.coroutines.android)

   // ❌ Wrong
   implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:+")
   ```

4. **Pin the Kotlin and JVM toolchain versions.**
   Use a modern stable Kotlin release and a JDK toolchain (17 or newer) declared once, in a convention plugin or the root build.

   ```kotlin
   kotlin { jvmToolchain(17) }
   ```

5. **Use the narrowest dependency configuration that works.**
   `implementation` over `api`; `testImplementation` / `androidTestImplementation` for test-only libraries; `ksp` / `kapt` only for annotation processors (prefer KSP).

6. **Use `./gradlew build` (or `check`) in CI/CD.**
   Reserve single-task commands (`testDebugUnitTest`, `assembleDebug`) for local iteration.

7. **Never commit build output or local config.**
   `build/`, `.gradle/`, and `local.properties` are always git-ignored. Never commit signing keystores or secrets.

## Why

A single build tool pinned through its wrapper, with versions centralized in one catalog, guarantees a reproducible dependency graph. Floating versions and scattered declarations produce builds that pass on one machine and fail on another.
