# Concurrency Rule

Use **Kotlin Coroutines and Flow** for all asynchronous work. Never manage raw threads, never block the main thread, and never leak a coroutine.

## Hard Rules

1. **Never create or start a raw `Thread`, `AsyncTask`, `Handler.post`, or `Executor` for application work.**
   Use coroutines. Use `WorkManager` for deferrable work that must survive process death.

   ```kotlin
   // ✅ Correct
   viewModelScope.launch { val customer = repository.fetch(id) }

   // ❌ Wrong — raw thread, no cancellation, no lifecycle
   Thread { repository.fetchBlocking(id) }.start()
   ```

2. **Never block the main thread.**
   No network, disk, or database call, and no `runBlocking`, on the main thread. Expose `suspend` functions that are *main-safe*: the function itself switches dispatcher with `withContext`, so callers never have to.

   ```kotlin
   // ✅ Correct — main-safe suspend function
   class SettingRepository(private val api: SettingApi, private val ioDispatcher: CoroutineDispatcher) {
       suspend fun getAll(): List<Setting> = withContext(ioDispatcher) { api.fetchAll() }
   }

   // ❌ Wrong — runBlocking freezes the UI and risks an ANR
   fun getAll(): List<Setting> = runBlocking { api.fetchAll() }
   ```

3. **Inject dispatchers — never hardcode `Dispatchers.IO` / `Default` / `Main` in business code.**
   Pass a `CoroutineDispatcher` (or a `DispatcherProvider`) through the constructor so tests can substitute a test dispatcher.

4. **Use structured concurrency — never `GlobalScope`.**
   Launch only from a lifecycle-aware scope: `viewModelScope`, `lifecycleScope`, `repeatOnLifecycle`, or a scope injected and cancelled by its owner. Long-lived app-scoped work uses an injected application `CoroutineScope` with a `SupervisorJob`.

   ```kotlin
   // ✅ Correct
   viewModelScope.launch { ... }

   // ❌ Wrong — outlives the screen, leaks, cannot be cancelled
   GlobalScope.launch { ... }
   ```

5. **Run independent work in parallel with `async` inside `coroutineScope`.**
   Never nest callbacks.

   ```kotlin
   // ✅ Correct — flat, parallel, cancelled together
   suspend fun loadSummary(orderId: String): OrderSummary = coroutineScope {
       val order = async { orderRepository.fetch(orderId) }
       val customer = async { customerRepository.fetch(orderId) }
       OrderSummary.of(order.await(), customer.await())
   }

   // ❌ Wrong — callback pyramid
   orderApi.fetch(orderId) { order -> customerApi.fetch(order.customerId) { customer -> ... } }
   ```

6. **Never swallow `CancellationException`.**
   Catching `Exception` or `Throwable` in a suspend function also catches cancellation. Rethrow it, or catch only specific types.

   ```kotlin
   // ✅ Correct
   try {
       api.fetch(id)
   } catch (e: CancellationException) {
       throw e
   } catch (e: IOException) {
       throw CustomerFetchException(id, e)
   }

   // ❌ Wrong — cancellation silently lost; the coroutine keeps running
   try { api.fetch(id) } catch (e: Exception) { null }
   ```

7. **Bound every wait on an external dependency with a timeout.**
   Use `withTimeout` / `withTimeoutOrNull` with a named constant (`FETCH_TIMEOUT`).

8. **Expose state as `StateFlow` and one-shot events as `SharedFlow` / `Channel` — never `LiveData` in new code, never mutable flows publicly.**
   Keep `MutableStateFlow` `private` and expose `asStateFlow()`. Collect in the UI with `collectAsStateWithLifecycle()` (Compose) or `repeatOnLifecycle(STARTED)` (Views) so collection stops in the background. Convert cold upstream flows with `stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), initial)`.

   ```kotlin
   // ✅ Correct
   private val _state = MutableStateFlow<UiState>(UiState.Loading)
   val state: StateFlow<UiState> = _state.asStateFlow()

   // ❌ Wrong — any caller can mutate the state
   val state = MutableStateFlow<UiState>(UiState.Loading)
   ```

9. **Prefer immutable state over shared mutable state and locks.**
   Share state through immutable `data class` values and `StateFlow.update { }`. When a mutable invariant is unavoidable, confine it to a single dispatcher or guard it with a `Mutex` — never `synchronized` around a suspending call.

## Why

Raw threads, unbounded waits, and ad-hoc locking produce failures that appear only on real devices under load — ANRs, leaks, and crashes after rotation. Structured concurrency ties every coroutine to a lifecycle, so work stops when the user leaves the screen, and test dispatchers make asynchronous code deterministic.
