# No Magic Numbers Rule

Every numeric (or string) literal that carries domain meaning must be extracted to a named constant. A bare number in the middle of logic silently encodes intent that the reader cannot recover without context.

## Hard Rules

1. **Never use a bare numeric or string literal where its meaning is not self-evident from immediate context.**

   ```kotlin
   // ✅ Correct — intent is explicit
   private const val MAX_RETRY_ATTEMPTS = 3
   private const val TENANT_HEADER = "x-tenant-id"

   if (attempt > MAX_RETRY_ATTEMPTS) throw RetryExhaustedException("[RetryPolicy] max retries exceeded")

   // ❌ Wrong
   if (attempt > 3) throw RetryExhaustedException("[RetryPolicy] max retries exceeded")
   ```

2. **Place constants as close as possible to their use, but no closer than needed.**
   - **File-scoped:** `private const val` at the top level of the file — the default choice in Kotlin.
   - **Class-scoped:** `private companion object { const val ... }` when the value belongs to one class.
   - **Shared:** a dedicated `object` (or an `enum`) only when several classes genuinely use the same value.
   - Do not make constants `public` / `internal` unless another file actually uses them.

3. **Use `const val` for compile-time primitives and strings; use typed values for everything else.**
   Durations are `Duration` (`5.seconds`, `24.hours`), never raw milliseconds. Sizes carry their unit in the name.

   ```kotlin
   // ✅ Correct
   private val SESSION_TTL = 24.hours
   private const val MAX_UPLOAD_SIZE_BYTES = 10_485_760L

   // ❌ Wrong
   private const val SESSION_TTL = 86400000L
   ```

4. **Never inline HTTP status codes, request codes, or intent/extra keys.**
   Use named constants (`HttpURLConnection.HTTP_CREATED`, Retrofit/Ktor status enums). Navigation routes and `Intent` extras are constants or typed route objects (type-safe Navigation), never repeated string literals.

5. **UI dimensions, colors, and text belong to the design system and resources — never inline.**
   - Sizes live in a theme/spacing object (`Spacing.medium`) or `dimens.xml`; never write `padding(16.dp)` repeatedly with an unnamed 16.
   - Colors come from `MaterialTheme.colorScheme` / the theme, never `Color(0xFF123456)` inline.
   - Text comes from `strings.xml`.

   ```kotlin
   // ✅ Correct
   Modifier.padding(Spacing.medium)

   // ❌ Wrong
   Modifier.padding(16.dp).background(Color(0xFF1A73E8))
   ```

6. **Prefer enums over groups of related constants.**

   ```kotlin
   // ✅ Correct
   enum class OrderStatus { ACTIVE, CANCELLED, SHIPPED }

   // ❌ Wrong
   const val STATUS_ACTIVE = 1
   const val STATUS_CANCELLED = 2
   ```

7. **The only exempt literals are universally self-evident values.**
   - `0` and `1` used as indices, counters, or loop seeds where context makes meaning obvious.
   - `true` / `false`, `""` as a default, `1f` / `0f` for alpha/weight-style fractions with a self-evident meaning.

   Any other numeric literal — including `2`, `-1`, `404`, `1000`, `86400` — requires a named constant.

8. **Use underscores for readability on large literals.**

   ```kotlin
   private const val MAX_UPLOAD_SIZE_BYTES = 10_485_760L
   ```

## Why

A magic number is a contract between the author and the future reader that the reader was never told about. A named constant makes the intent explicit, creates a single point of change, and — for UI values — keeps the design consistent and themeable across every screen and device size.
