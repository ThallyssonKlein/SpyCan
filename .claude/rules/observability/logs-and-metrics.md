---
name: Logs and Metrics Rule
description: Every implementation class in src/main (ViewModels, use cases, repositories, clients — not interfaces, models, composables, or tests) must include structured logs and metrics.
type: feedback
---

# Logs and Metrics Rule

When writing or editing any implementation class under `src/main` (ViewModels, use cases, repositories, API clients, workers), **always include both structured logs and metrics**.

## Scope

Applies to: `src/main/**/*.kt` — except:
- Interfaces
- `data class`, `enum class`, `sealed` state/event types, and other data carriers
- `@Composable` functions and `Activity` / `Fragment` glue (report screen views through the navigation layer or a ViewModel instead)
- Exceptions, DI modules, and configuration classes
- `src/test/**` and `src/androidTest/**`

## Required Patterns

### Logs

Use Timber (or the project's configured logging facade) through direct calls. The message prefix rule is defined in `log-class-prefix.md`.

```kotlin
Timber.i("[SettingRepository] descriptive message")
Timber.w("[SettingRepository] descriptive message for id: %s", id)
Timber.e(exception, "[SettingRepository] descriptive message")
```

### Metrics

Use the project's configured analytics / performance library (Firebase Analytics, Firebase Performance, Sentry, OpenTelemetry…) **behind an interface**, injected through the constructor — never call the SDK singleton directly from business code. Define event and parameter names as constants, and keep cardinality low (no ids, emails, or free text as parameter values).

```kotlin
interface Metrics {
    fun count(name: String, vararg tags: Pair<String, String>)
    suspend fun <T> time(name: String, block: suspend () -> T): T
}

class SettingRepository(
    private val api: SettingApi,
    private val metrics: Metrics,
) {
    suspend fun create(data: SettingBody) {
        api.create(data)
        metrics.count(OPERATIONS_METRIC, "operation" to "create", "status" to "ok")
    }

    private companion object {
        const val OPERATIONS_METRIC = "setting_operations"
    }
}
```

Use a timer for latency of network calls and expensive local work (`metrics.time(...)`), and record non-fatal exceptions to the crash reporter at the boundary (see `error-handling.md`).

> The snippets above are illustrative. Use whichever logging and metrics libraries the project already adopts.

## Privacy

Mobile metrics leave the user's device. Respect the user's analytics-consent setting before sending events, and never attach personal data to events or logs.

## Reference Implementation

Point to the project's canonical class that applies both patterns together (for example `app/src/main/kotlin/<base-package>/data/SettingRepository.kt`) and keep this reference up to date.
