---
name: Verify After Edit Rule
description: After modifying any file under src/main, src/test or src/androidTest, run format, static analysis, the affected tests, and the full build — fix all failures before finishing.
type: feedback
---

# Verify After Edit Rule

After **any** modification to Kotlin or resource files in the app, you must run the formatter, static analysis, the affected tests, and the build. Do not consider a task done until all pass.

## Hard Rules

1. **Only after ALL edits are done — run format, then lint/detekt, then the affected tests, then the build.**
   Do not run these between individual file edits. Wait until the full change set is complete.

   ```bash
   ./gradlew ktlintFormat
   ./gradlew detekt lintDebug
   ./gradlew testDebugUnitTest --tests "com.acme.SettingRepositoryTest"
   ./gradlew build
   ```

2. **If formatting or static analysis fails — fix all findings before proceeding.**
   Run `ktlintFormat` to auto-fix what's possible, then manually resolve remaining detekt / Android Lint issues. Never add `@Suppress` or baseline entries just to silence a new finding.

3. **If tests fail — fix the failures before finishing.**
   Read the output, find the root cause, fix the code or the test, and re-run until green. Never `@Ignore` / `@Disabled` a test or use `-x test` to get past a failure.

4. **If the build fails — fix the compiler errors before finishing.**
   Do not ignore warnings promoted to errors (`allWarningsAsErrors`). Re-run `./gradlew build` until it exits with code 0.

5. **Do not skip verification even for "small" edits.**
   A one-line change can break a lint rule, a type, or a test.

6. **UI changes are not verified by compilation alone.**
   For changes to screens, run the relevant Compose UI / screenshot tests, and launch the app on an emulator when the change affects navigation, permissions, or lifecycle behavior.

## Order of Operations

```
1. Make ALL code changes
2. ./gradlew ktlintFormat                 ← fix until clean
3. ./gradlew detekt lintDebug             ← fix until clean
4. ./gradlew testDebugUnitTest --tests …  ← fix until green
5. ./gradlew build                        ← fix until exits 0
6. Task is done
```

## Why

Unverified changes break the pipeline and shift debugging burden to the next person. Running every gate locally — and fixing failures immediately — keeps the branch always green.
