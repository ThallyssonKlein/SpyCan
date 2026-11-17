# Functions Rule

Functions must be small, single-purpose, and predictable.

## Hard Rules

1. **A function does exactly one thing.**
   If you can describe what it does with "and", split it.

2. **Keep all statements at the same abstraction level.**
   A use case orchestrates calls to repositories — it does not format strings or parse dates inline.

3. **Limit parameters to three. Group anything beyond that into a `data class`, or use named/default arguments.**
   Always call functions with more than one parameter using **named arguments** when the types are not self-evident. Composables are the exception to the limit: they may take many parameters, but must order them as `modifier` first among optional ones, and event lambdas last.

   ```kotlin
   // ✅ Correct
   data class PublishEventParams(val tenantId: String, val eventType: String, val payload: Map<String, Any>)
   fun publishEvent(params: PublishEventParams)

   // ❌ Wrong
   fun publishEvent(tenantId: String, eventType: String, payload: Map<String, Any>, retry: Boolean, timeout: Long)
   ```

4. **Never use boolean flag parameters.**
   A `Boolean` argument means the function does two things. Split it into two functions (or use an `enum`).

   ```kotlin
   // ✅ Correct
   fun renderForPrint(document: Document)
   fun renderForScreen(document: Document)

   // ❌ Wrong
   fun render(document: Document, forPrint: Boolean)
   ```

5. **No hidden side effects.**
   A function must only do what its name promises. Do not log, mutate external state, or call unrelated services inside a function that claims to do something else. In Compose, side effects belong in `LaunchedEffect` / `DisposableEffect` / `SideEffect` — never in the composable body.

6. **Obey Command-Query Separation.**
   Functions that change state return `Unit`. Functions that return data do not change state.

   ```kotlin
   // ✅ Correct
   suspend fun deleteById(id: String)
   suspend fun findById(id: String): Customer?
   ```

7. **Prefer pure functions and immutable arguments.**
   Parameters are `val` by definition in Kotlin — never mutate an argument the caller owns (e.g. a `MutableList` parameter). Prefer expression bodies (`= ...`) for single-expression functions, and extension functions for stateless transformations over a type you do not own.

8. **Make functions `suspend` instead of callback-based.**
   Wrap legacy callbacks with `suspendCancellableCoroutine` / `callbackFlow` once, at the boundary.

## Why

A function that does one thing at one abstraction level can be named precisely, tested in isolation, and replaced without side effects. Functions that mix concerns, accumulate parameters, or violate CQS become the source of bugs that are hard to trace and tests that are hard to write.
