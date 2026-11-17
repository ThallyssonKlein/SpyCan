---
name: Coverage Rule
description: Implementation classes in src/main must have >= 80% unit test coverage across instructions, branches, and lines (Kover).
type: feedback
---

# Coverage Rule

When writing or editing implementation classes under `src/main` (ViewModels, use cases, repositories, data sources, mappers), ensure the corresponding test class covers **>= 80%** on instructions, branches, and lines, measured by **Kover**.

## How to apply

- Write comprehensive tests upfront — cover happy path, error paths, and edge cases.
- If editing an existing class, check whether new branches are already covered and add the missing cases.
- The test class mirrors the source path and package: `src/main/kotlin/com/acme/SettingRepository.kt` → `src/test/kotlin/com/acme/SettingRepositoryTest.kt`.
- Pure UI code (`@Composable` functions, `Activity`/`Fragment` glue, DI modules, generated code) is excluded from the threshold; cover it with UI/screenshot tests instead. Keep logic out of it so the exclusion stays honest.

## Verification

```bash
./gradlew koverHtmlReportDebug
# report: build/reports/kover/htmlDebug/index.html
```

Enforce the threshold in the build with `koverVerify` (`minBound(80)` on `LINE`, `BRANCH`, and `INSTRUCTION`) so a drop below 80% fails `./gradlew build`.
