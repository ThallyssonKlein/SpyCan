# Log Class Prefix Rule

Every log statement must be prefixed with `[ClassName]` — the exact name of the class where the log is written.

## Hard Rules

1. **All `Timber.*` calls must start with `[ClassName]` at the beginning of the message.**
   Use the literal class name, not a variable, alias, or `this::class.simpleName`.

2. **The prefix must be the exact class name, PascalCase, in square brackets.**
   Use Timber's format arguments — never string concatenation or interpolation of dynamic values.

   ```kotlin
   // ✅ Correct
   Timber.i("[OrderViewModel] screen opened")
   Timber.w("[OrderRepository] order not found for id: %s", orderId)
   Timber.e(exception, "[OrderApiClient] failed to submit order")

   // ❌ Wrong — no prefix
   Timber.i("screen opened")

   // ❌ Wrong — wrong format
   Timber.i("OrderViewModel: screen opened")
   Timber.i("[orderViewModel] screen opened")

   // ❌ Wrong — string template instead of format arguments
   Timber.w("[OrderRepository] order not found for id: $orderId")
   ```

3. **For `Timber.e` / `Timber.w` with a failure, pass the throwable as the FIRST argument.**
   Timber prints the stack trace when it receives the `Throwable`. Never log only `e.message`.

   ```kotlin
   // ✅ Correct
   Timber.e(exception, "[ClassName] operation failed")

   // ❌ Wrong — stack trace lost
   Timber.e("[ClassName] operation failed: %s", exception.message)

   // ❌ Wrong — exception swallowed
   Timber.e("[ClassName] operation failed")
   ```

4. **The message after the prefix must be descriptive and in English.**
   Avoid generic messages like `[ClassName] error` or `[ClassName] ok`.

5. **Use the right level.**
   `e` for failures that need attention, `w` for recoverable anomalies, `i` for significant business events, `d` / `v` for diagnostics (stripped from release builds). Never log secrets, tokens, or personal data (emails, phone numbers, location, device identifiers).

6. **Never use `android.util.Log` or `println` directly.**
   Log only through Timber (or the project's logging facade). Plant a `DebugTree` only in debug builds and a crash-reporting tree in release.

## Why

A consistent `[ClassName]` prefix makes logcat lines instantly filterable and traceable to their source — important on mobile, where ProGuard/R8 obfuscation makes automatic tags useless in release builds.
