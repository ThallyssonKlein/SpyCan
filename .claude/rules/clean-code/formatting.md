# Formatting Rule

Code must be readable top-to-bottom. Structure and placement are part of the design.

## Hard Rules

1. **Apply the Newspaper Metaphor.**
   High-level concepts appear first and implementation details emerge as you read down. Within a class: constants (`companion object` at the bottom per Kotlin convention, or top-level `private const val`), properties, `init`, public functions, then private helpers.

   ```kotlin
   // ✅ Correct — public API at top, private helpers at bottom
   class EventProducerService {
       fun publishEvent(params: PublishEventParams) { ... }

       private fun buildPayload(params: PublishEventParams): EventPayload { ... }
       private fun validateTenant(tenantId: String) { ... }
   }
   ```

2. **Keep related concepts vertically close.**
   A caller and its callee belong near each other. Do not scatter related functions across the file.

3. **Declare variables as close as possible to their first use.**
   Prefer `val` over `var`; never reuse a variable for a different meaning.

   ```kotlin
   // ✅ Correct
   fun publishEvent(params: PublishEventParams) {
       validateTenant(params.tenantId)

       val payload = buildPayload(params)
       eventRepository.publish(payload)
   }
   ```

4. **Use an automatic formatter and never format by hand.**
   ktlint (or Spotless with ktlint) with the official Kotlin style (`.editorconfig`) is the single source of truth. Run it before finishing; do not commit unformatted code.

5. **One primary type per file, file named after it, and no wildcard imports.**
   Small closely related declarations (a `sealed interface` and its variants, a state + its events) may share a file. Import each symbol explicitly; remove unused imports.

6. **Structure the Compose UI for readability.**
   A `@Composable` screen is split into a stateful entry point (collects state, owns the ViewModel) and a stateless content function (takes state and lambdas, easy to preview and test). Keep `@Preview` functions private and at the bottom of the file.

   ```kotlin
   // ✅ Correct — stateful wrapper + stateless content
   @Composable
   fun OrdersScreen(viewModel: OrdersViewModel = hiltViewModel()) {
       val state by viewModel.state.collectAsStateWithLifecycle()
       OrdersContent(state = state, onOrderClick = viewModel::onOrderClick)
   }

   @Composable
   private fun OrdersContent(state: OrdersState, onOrderClick: (String) -> Unit) { ... }
   ```

## Why

Code that flows top-to-bottom mirrors how people read. Separating stateful and stateless composables keeps UI previewable and testable, and a single automated formatter ends style debates in review.
