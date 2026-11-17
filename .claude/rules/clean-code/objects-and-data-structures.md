# Objects and Data Structures Rule

Objects hide data behind abstractions and expose behavior. Data structures expose data and carry no meaningful behavior. Never mix these two roles in the same type.

## Hard Rules

1. **Expose abstractions, not internals.**
   A class must not expose its state through raw mutable properties. Expose behavior that lets callers manipulate the *essence* of the data.

   ```kotlin
   // ✅ Correct
   interface FuelGauge { val percentFull: Double }

   // ❌ Wrong — exposes raw mutable state
   class Vehicle { var gallonsOfGasoline: Double = 0.0 }
   ```

2. **Obey the Law of Demeter.**
   A function may only call functions on: its own class, objects passed as arguments, objects it creates directly, or direct component properties. Never chain calls into objects returned by other calls.

   ```kotlin
   // ✅ Correct
   class OrderViewModel(private val submitOrder: SubmitOrderUseCase) {
       fun onSubmitClicked(order: Order) = viewModelScope.launch { submitOrder(order) }
   }

   // ❌ Wrong — reaches into internals through a chain
   val connectionString = context.environment.config.database.connectionString
   ```

3. **Never mix object and data-structure roles.**
   A type is either an object (hides data, exposes behavior) or a data structure (exposes data, has no behavior). A `data class` with real business logic and `var` properties is a hybrid.

   ```kotlin
   // ✅ Correct — pure data structure
   data class PublishEventBody(val tenantId: String, val eventType: String, val payload: Map<String, Any>)

   // ✅ Correct — pure object: dependencies hidden, behavior exposed
   class EventProducerService(private val publisher: EventPublisher) {
       fun publishEvent(body: PublishEventBody) { ... }
   }

   // ❌ Wrong — mutable public data with business logic
   data class Event(var tenantId: String, var payload: MutableMap<String, Any>) {
       fun publish() { ... }
   }
   ```

4. **Use the right Kotlin construct for each role.**
   - **`data class`** with `val` properties: DTOs, API/DB models, domain value objects, UI state. Validate invariants in `init { require(...) }`.
   - **`@JvmInline value class`**: typed identifiers and units (`UserId`, `Cents`) to prevent mixing primitives.
   - **`sealed interface` + `data class` / `data object`**: closed variants (UI state, results, navigation events, intents), consumed with an exhaustive `when`.
   - **`enum class`**: closed sets of constants without per-case data.
   - **`class`**: ViewModels, use cases, repositories, and any type that encapsulates behavior behind an interface.
   - **`object`**: stateless singletons only. Never hold mutable state or Android `Context` in an `object`.

   ```kotlin
   // ✅ Correct — invariant validated once
   data class Money(val amount: BigDecimal, val currency: Currency) {
       init { require(amount.signum() >= 0) { "[Money] amount must not be negative" } }
   }

   // ✅ Correct — sealed UI state, consumed exhaustively
   sealed interface OrdersUiState {
       data object Loading : OrdersUiState
       data class Success(val orders: List<Order>) : OrdersUiState
       data class Error(@StringRes val messageRes: Int) : OrdersUiState
   }
   ```

5. **Keep the layers' models separate and map between them.**
   Network DTOs (`@Serializable`), database entities (`@Entity`), domain models, and UI models are different types. Map at the layer boundary with extension functions (`OrderDto.toDomain()`); never leak an `@Entity` or DTO into the UI.

6. **Prefer immutability.**
   `val` over `var`, read-only collections, and `copy()` for updates. UI state is a single immutable `data class` / `sealed` type replaced atomically (`_state.update { it.copy(...) }`), never mutated in place — this is also what keeps Compose recomposition correct.

7. **Never put `Context`, `View`, or `Activity` references in a ViewModel, use case, repository, or any long-lived object.**
   They leak the screen. Inject an application-scoped abstraction (`ResourceProvider`, `Clock`, `PermissionChecker`) instead.

## Why

Mixing roles produces types that are hard to change and hard to test. Keeping data structures immutable and separate per layer prevents leaks between the database, network, and UI, and makes state changes explicit — which is the foundation of predictable mobile UI.
