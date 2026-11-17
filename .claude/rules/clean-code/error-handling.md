# Error Handling Rule

Errors are part of the domain. Handle them explicitly, with context, and without hiding failures behind null or error codes.

## Hard Rules

1. **Prefer exceptions (or typed results) to error codes.**
   Never return a sentinel value (`null`, `-1`, `false`) to signal an unexpected failure — it forces every caller to inspect the return value and silently propagates failures when callers forget.

   ```kotlin
   // ✅ Correct
   suspend fun fetchById(id: String): Customer =
       dao.findById(id)?.toDomain() ?: throw CustomerNotFoundException(id)

   // ❌ Wrong — caller must remember to check for null
   suspend fun fetchById(id: String): Customer? = dao.findById(id)?.toDomain()
   ```

2. **Provide context in every exception.**
   Every thrown exception must identify the class, the operation, and what went wrong. Use dedicated exception types and keep the failing value as a property.

   ```kotlin
   // ✅ Correct — dedicated type, message includes class and failing value
   class CustomerNotFoundException(val customerId: String) :
       RuntimeException("[CustomerRepository] Customer not found for id: $customerId")

   // ❌ Wrong — no origin, no context
   throw RuntimeException("Not found")
   ```

3. **Use Kotlin null-safety honestly. Never use `!!`. Never return `null` to mean "failed".**
   `null` is allowed when absence is a normal, expected outcome of the query (`findById`, `firstOrNull`) and the type says so (`T?`). It is never a failure signal. Return empty collections instead of `null`. Never accept a nullable parameter just to mean "not provided" when a default argument or overload expresses it.

   ```kotlin
   // ✅ Correct — absence is part of the contract, expressed in the type
   suspend fun findById(id: String): Customer?
   fun findByCustomer(customerId: String): List<Order>   // empty list when none

   // ✅ Correct — unwrap with intent
   val customer = repository.findById(id) ?: throw CustomerNotFoundException(id)
   val name = requireNotNull(user.name) { "[UserMapper] name is required" }

   // ❌ Wrong — !! turns a missing value into an unexplained NullPointerException
   val name = user.name!!

   // ❌ Wrong — lateinit/platform types used to dodge null checks on Java APIs; annotate or wrap at the boundary
   ```

4. **Model expected failures as typed results; reserve exceptions for the unexpected.**
   For failures the UI must react to (no network, validation, unauthorized), return a `sealed` result from the repository/use case. Reserve exceptions for programmer errors and unrecoverable states.

   ```kotlin
   // ✅ Correct — failure is part of the type, exhaustively handled
   sealed interface LoginResult {
       data class Success(val session: Session) : LoginResult
       data object InvalidCredentials : LoginResult
       data object NoConnection : LoginResult
   }

   when (val result = login(credentials)) {
       is LoginResult.Success -> navigateHome(result.session)
       LoginResult.InvalidCredentials -> showInvalidCredentials()
       LoginResult.NoConnection -> showOffline()
   }
   ```

5. **Wrap platform and library exceptions once, at the boundary, preserving the cause.**
   Translate `IOException`, `HttpException`, `SQLiteException`, `SerializationException` into domain errors where the data layer meets the domain.

   ```kotlin
   // ✅ Correct — wrapped once, cause preserved
   try {
       api.fetch(id)
   } catch (e: IOException) {
       throw NetworkUnavailableException("[SettingRepository] network unavailable", e)
   }

   // ❌ Wrong — cause lost
   } catch (e: IOException) {
       throw NetworkUnavailableException("network unavailable")
   }
   ```

6. **Catch the most specific exception. Never catch `Exception` or `Throwable` to hide a failure, never leave a `catch` empty, and never use `runCatching` around suspend code without rethrowing `CancellationException`.**

   ```kotlin
   // ✅ Correct
   } catch (e: CustomerNotFoundException) {
       _state.value = UiState.NotFound
   }

   // ❌ Wrong — swallows every failure, including cancellation
   } catch (e: Exception) {
       // ignore
   }
   ```

7. **Always release resources with `use { }`.**
   Never rely on a `finally` that calls `close()` manually (cursors, streams, `Closeable`).

   ```kotlin
   // ✅ Correct
   context.contentResolver.openInputStream(uri)?.use { parse(it) }
   ```

8. **Handle failures at the boundary: the ViewModel turns them into UI state, and logs/reports once.**
   Repositories and use cases let exceptions propagate. The ViewModel (or a `CoroutineExceptionHandler` at the scope root) catches, logs **once**, records a non-fatal to the crash reporter, and maps the failure to an error `UiState` or one-shot event. Never both log and rethrow. Never show raw exception messages to users.

   ```kotlin
   // ✅ Correct — one place logs, one place maps to UI
   viewModelScope.launch {
       try {
           _state.value = UiState.Success(getCustomer(id))
       } catch (e: CustomerNotFoundException) {
           Timber.w(e, "[CustomerViewModel] customer not found")
           _state.value = UiState.NotFound
       }
   }

   // ❌ Wrong — repository also logs the same error and rethrows
   } catch (e: CustomerNotFoundException) {
       Timber.e(e, "[CustomerRepository] failed to fetch customer") // duplicate
       throw e
   }
   ```

## Why

Error codes scatter failure-handling across every call site. Null-as-failure hides the moment a failure occurred, and `!!` converts it into a crash with no context — on mobile, a crash is a lost user. Typed results make every expected failure visible to the compiler, and exceptions with context make the unexpected ones traceable to a single origin.
